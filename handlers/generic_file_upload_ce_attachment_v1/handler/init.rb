require "base64"
require "cgi"
require 'erb'
require 'fileutils'
require 'json'
require 'net/https'
require 'securerandom'
require 'tmpdir'


class GenericFileUploadCeAttachmentV1
  def initialize(input)
    
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Store the info values in a Hash of info names to values.
    @info_values = {}
    REXML::XPath.each(@input_document,"/handler/infos/info") do |item|
      @info_values[item.attributes['name']] = item.text
    end

    # Retrieve all of the handler parameters and store them in a hash attribute
    # named @parameters.
    @parameters = {}
    REXML::XPath.match(@input_document, 'handler/parameters/parameter').each do |node|
      @parameters[node.attribute('name').value] = node.text.to_s
    end

    @enable_debug_logging = ["yes", "true"].include?(@info_values['enable_debug_logging'].downcase)
    @raise_error = @parameters["error_handling"] == "Raise Error"

    puts "Parameters: #{@parameters.inspect}" if @enable_debug_logging
  end


  def execute()
    space_slug = @parameters["space_slug"].empty? ? @info_values["space_slug"] : @parameters["space_slug"]
    if @info_values['api_server'].include?("${space}")
      server = @info_values['api_server'].gsub("${space}", space_slug)
    elsif !space_slug.to_s.empty?
      server = @info_values['api_server']+"/"+space_slug
    else
      server = @info_values['api_server']
    end

    #Server Variables
    user             = @info_values["api_username"]
    pass             = @info_values["api_password"]
    #Source Submission Variables
    submission_id    = @parameters["submission_id"]
    field_name       = @parameters["field_name"]
    #Destination
    destination_api  = @info_values["destination_api"]
    destination_token_name = @info_values["destination_token_name"]
    destination_token_value =  @info_values["destination_token_value"]
    attachment_index = @parameters["attachment_index"]
    file_part_parameter = @parameters["file_part_parameter"]


    # Parse the multipart parameter options
    begin
      multipart_params = @parameters["multipart_params"].strip.size > 0 ?
        JSON.parse(@parameters["multipart_params"]) :
        {}
    rescue JSON::ParserError => e
      message = "The multipart params hash parameter is not valid JSON"
      return handle_exception(message, e)
    end


    # Headers for server: Authorization, Accept, Content-Type
    headers = http_basic_headers(user, pass)


    # Retrieve the source submission
    puts "Retrieving source submission: #{submission_id}" if @enable_debug_logging
    # Submission API Route
    source_submission_route = "#{server}/app/api/v1/submissions/#{submission_id}/?include=values"
    # Retrieve the submission and values
    res = http_get(source_submission_route, { "include" => "values" }, headers)
    if !res.kind_of?(Net::HTTPSuccess)
      message = "Failed to retrieve source submission #{submission_id}"
      return handle_exception(message, res)
    end
    submission = JSON.parse(res.body)["submission"]
    puts "Received source submission #{submission['id']}" if @enable_debug_logging


    # Check if the there are any attachments in the source field
    field_value = submission["values"][field_name]

    # If the attachment field value exists
    if !field_value.nil?
      # Attachment field values are stored as arrays, one map for each file attachment.
      #
      # This isn't the real attachment info though, this is just metadata about the attachment
      # that can be retrieved to get a link to the attachment in Filehub.
      #
      # Process each attachment file
      # field_value.each_with_index do |attachment_info, index|
      # attachment_index is a value yet to be created
      

      attachment_info = field_value[attachment_index.to_i]
      begin
        # The attachment file name is stored in the 'name' property
        attachment_name = attachment_info['name']

        # Temporary file to stream contents to
        tempdir = "#{Dir.tmpdir}/#{SecureRandom.hex(8)}"
        tempfile = "#{tempdir}/#{attachment_name}"
        FileUtils.mkdir_p(tempdir)


        # Retrieve the attachment download link from the server
        puts "Retrieving attachment download link from source submission: #{attachment_name} for field #{field_name}" if @enable_debug_logging

        # API route to get the generated attachment download link from Kinetic Request CE.
        # "/{spaceSlug}/app/api/v1/submissions/{submissionId}/files/{fieldName}/{fileIndex}/{fileName}/url"
        download_link_api_route = "#{server}/app/api/v1" <<
          "/submissions/#{submission_id}" <<
          "/files/#{URI.escape(field_name)}" <<
          "/#{attachment_index.to_i}/#{URI.escape(attachment_name)}/url"


        # Retrieve the URL to download the attachment from Kinetic Request CE.
        # This URL will only be valid for a short amount of time before it expires
        # (usually about 5 seconds).
        res = http_get(download_link_api_route, {}, headers)
        if !res.kind_of?(Net::HTTPSuccess)
          message = "Failed to retrieve link for attachment #{attachment_name} from source submission"
          return handle_exception(message, res)
        end
        file_download_url = JSON.parse(res.body)['url']
        puts "Received link for attachment #{attachment_name} from source submission" if @enable_debug_logging


        # Inspect the attachment URL to determine if using FileHub or Agent
        attachment_uri = URI(file_download_url)
        query_params = CGI::parse(attachment_uri.query || "")
        # If url contains a signature query parameter, using FileHub (no authorization header)
        filestore_headers = query_params.has_key?("signature") && !query_params["signature"].empty? ? {} : headers


        # Download the attachment from the source submission
        puts "Downloading attachment #{attachment_name} from #{file_download_url}" if @enable_debug_logging
        res = stream_file_download(tempfile, file_download_url, {}, filestore_headers)
        if !res.kind_of?(Net::HTTPSuccess)
          message = "Failed to download attachment #{attachment_name} from the filestore server"
          return handle_exception(message, res)
        end

        puts "Uploading attachment file: #{attachment_name} to #{destination_api}" if @enable_debug_logging

        # Add the upload query parameters to the upload URL
        if destination_token_name.to_s.strip.size > 0
          file_upload_url = destination_api.include?("?") ?
          destination_api << "&#{CGI::escape(destination_token_name)}=#{CGI::escape(destination_token_value)}" :
          destination_api << "?#{CGI::escape(destination_token_name)}=#{CGI::escape(destination_token_value)}"
        else
          file_upload_url = destination_api
          puts "No destination token name provided." if @enable_debug_logging
        end

        # Upload form parameters
        form_params = multipart_params.merge({ file_part_parameter => File.open(tempfile) })

        # Upload the attachment
        res = stream_file_upload(file_upload_url, form_params)
        if !res.kind_of?(Net::HTTPSuccess)
          message = "Failed to upload attachment #{attachment_name} to the server"
          return handle_exception(message, res)
        end

        puts "Successfully uploaded file." if @enable_debug_logging
        response_body = res.body

      ensure
        # Remove the temp directory along with the downloaded attachment
        FileUtils.rm_rf(tempdir)
      end
      
    else
      puts "Source submission attachment field value is empty: #{field_name}" if @enable_debug_logging
    end

    results = handle_results("", attachment_name, response_body)
    puts "Returning results: #{results}" if @enable_debug_logging
    results
  end



  def handle_results(error_msg, filename, response_body)
    <<-RESULTS
    <results>
      <result name="Handler Error Message">#{ERB::Util.html_escape(error_msg)}</result>
      <result name="File">#{ERB::Util.html_escape(filename)}</result>
      <result name="Response Body">#{ERB::Util.html_escape(response_body)}</result>
    </results>
    RESULTS
  end



  def handle_exception(message, error)
    case error
    when Net::HTTPResponse
      begin
        content = JSON.parse(error.body)
        error_key = content["errorKey"] || error.code
        error_msg = content["error"] || ""
        error_message = "#{message}:\n\tError Key: #{error_key}\n\tError: #{error_msg}"
      rescue StandardError => e
        error_key = error.code
        error_message = "#{message}:\n\tError Key: #{error_key}\n\tError: #{error.body}"
      end
    when NilClass
      error_message = "0: No response from server"
    when JSON::ParserError
      error_message = message
    else
      error_message = "Unexpected error: #{error.inspect}"
    end
    puts error_message
    raise error_message if @raise_error
    handle_results(error_message, nil, nil)
  end



  #-----------------------------------------------------------------------------
  # The following Http helper methods are provided within this handler because
  # task currently doesn't have a common http client module that handlers can
  # use. If these methods were packaged as a module within the dependencies.rb
  # file or within a gem/library, they would be under the same constraints as
  # other vendor gems, such as RestClient, where any handler that uses
  # RestClient is currently stuck using v1.6.7. Adding these methods
  # directly to the handler class gives the freedom to add/modify as needed
  # without affecting other handlers.
  #-----------------------------------------------------------------------------


  #-----------------------------------------------------------------------------
  # HTTP HEADERS
  #-----------------------------------------------------------------------------

  def http_json_headers
    {
      "Accept" => "application/json",
      "Content-Type" => "application/json"
    }
  end


  def http_basic_headers(username, password)
    http_json_headers.merge({
      "Authorization" => "Basic #{Base64.strict_encode64("#{username}:#{password}")}"
    })
  end


  #-----------------------------------------------------------------------------
  # REST ACTIONS
  #-----------------------------------------------------------------------------

  def http_delete(url, payload, parameters, headers, http_options={})
    uri = URI.parse(url)
    uri.query = URI.encode_www_form(parameters) unless parameters.empty?
    request = Net::HTTP::Delete.new(uri, headers)
    request.body = payload.to_json unless payload.nil? || payload.kind_of?(String)
    send_request(request, http_options)
  end


  def http_get(url, parameters, headers, http_options={})
    uri = URI.parse(url)
    uri.query = URI.encode_www_form(parameters) unless parameters.empty?
    request = Net::HTTP::Get.new(uri, headers)
    send_request(request, http_options)
  end


  def http_post(url, payload, parameters, headers, http_options={})
    payload = payload.to_json unless payload.is_a? String
    uri = URI.parse(url)
    uri.query = URI.encode_www_form(parameters) unless parameters.empty?
    request = Net::HTTP::Post.new(uri, headers)
    request.body = payload
    send_request(request, http_options)
  end


  def http_put(url, payload, parameters, headers, http_options={})
    payload = payload.to_json unless payload.is_a? String
    uri = URI.parse(url)
    uri.query = URI.encode_www_form(parameters) unless parameters.empty?
    request = Net::HTTP::Put.new(uri, headers)
    request.body = payload
    send_request(request, http_options)
  end


  #-----------------------------------------------------------------------------
  # ATTACHMENT METHODS
  #-----------------------------------------------------------------------------

  def stream_file_download(file, url, parameters, headers, http_options={})
    uri = URI.parse(url)
    uri.query = URI.encode_www_form(parameters) unless parameters.empty?

    http = build_http(uri, http_options)
    request = Net::HTTP::Get.new(uri, headers)

    http.request(request) do |response|
      open(file, 'w') do |io|
        response.read_body do |chunk|
          io.write chunk
        end
      end
    end
  end


  def stream_file_upload(url, form_parameters, http_options={})
    uri = URI.parse(url)

    request = Net::HTTP::Post.new(uri)
    request.set_form(form_parameters.to_a, "multipart/form-data")

    Net::HTTP.start(uri.hostname, uri.port, use_ssl: uri.scheme == 'https') do |http|
      configure_http(http, http_options)
      http.request(request)
    end
  end


  #-----------------------------------------------------------------------------
  # LOWER LEVEL METHODS
  #-----------------------------------------------------------------------------

  def send_request(request, http_options={})
    uri = request.uri
    Net::HTTP.start(uri.hostname, uri.port, use_ssl: uri.scheme == 'https') do |http|
      configure_http(http, http_options)
      http.request(request)
    end
  end


  def build_http(uri, http_options={})
    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl= true if (uri.scheme == 'https')
    configure_http(http, http_options)
    http
  end


  def configure_http(http, http_options={})
    http_options_sym = (http_options || {}).inject({}) { |h, (k,v)| h[k.to_sym] = v; h }
    http.verify_mode = http_options_sym[:ssl_verify] || OpenSSL::SSL::VERIFY_PEER if http.use_ssl?
    http.read_timeout= http_options_sym[:read_timeout] unless http_options_sym[:read_timeout].nil?
    http.open_timeout= http_options_sym[:open_timeout] unless http_options_sym[:open_timeout].nil?
  end

end
