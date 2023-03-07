# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class UtilityXmlToPdfV1

  # ==== Parameters
  # * +input+ - The String of Xml that was built by evaluating the node.xml handler template.
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Retrieve all of the handler info values and store them in a hash variable named @info_values.
    @info_values = {}
    REXML::XPath.each(@input_document, "/handler/infos/info") do |item|
      @info_values[item.attributes["name"]] = item.text.to_s.strip
    end

    # Retrieve all of the handler parameters and store them in a hash variable named @parameters.
    @parameters = {}
    REXML::XPath.each(@input_document, "/handler/parameters/parameter") do |item|
      @parameters[item.attributes["name"]] = item.text.to_s.strip
    end

    @debug_logging_enabled = ["yes","true"].include?(@info_values['enable_debug_logging'].downcase)
    @error_handling = @parameters["error_handling"]

    @api_location = @info_values["api_location"]
    @api_location.chomp!("/")
    @api_username = @info_values["api_username"]
    @api_password = @info_values["api_password"]

    @filename = @parameters['filename'].chomp('.pdf')
  end

  def execute
    # Initialize return data
    error_message = nil
    error_key = nil
    response_code = nil

    begin
      # Image File(s) variables
      files = []

      #Retrieve Image url
      #Submission API Route including Values
      submission_api_route = "#{@api_location}/submissions/"+
        "#{URI.escape(@parameters['submission_id'])}?include=values"
      puts "Route to Submission with Attachment File: #{submission_api_route}" if @debug_logging_enabled

      # Retrieve the Submission Values
      submission_result = RestClient::Resource.new(
        submission_api_route,
        user: @info_values["api_username"],
        password: @info_values["api_password"]
      ).get

      # If the submission exists
      unless submission_result.nil?
        submission = JSON.parse(submission_result)["submission"]
        field_value = submission["values"][@parameters["attachment_field_name"]]
        # If the attachment field value exists
        unless field_value.nil?
          # Attachment field values are stored as arrays, one map for each file attachment
          field_value.each_index do |index|
            file_info = field_value[index]
            # API route to get the generated attachment download link from Kinetic Request CE.
            attachment_download_api_route = @api_location +
              '/submissions/' + URI.escape(@parameters['submission_id']) +
              '/files/' + URI.escape(@parameters['attachment_field_name']) +
              '/' + index.to_s +
              '/' + URI.escape(file_info['name']) +
              '/url'

            # Retrieve the URL to download the attachment from Kinetic Request CE.
            attachment_download_result = RestClient::Resource.new(
              attachment_download_api_route,
              user: @info_values["api_username"],
              password: @info_values["api_password"]
            ).get

            unless attachment_download_result.nil?
              url = JSON.parse(attachment_download_result)['url']
              file_info["url"] = url
            end
            file_info.delete("link")
            files << file_info
          end
        end
      end

      unless files.nil?
        temp_sig_file = Tempfile.new(['file', '.png'], binmode: true)
        download = open(files[0]['url'],:http_basic_authentication=>[@info_values["api_username"],@info_values["api_password"]])
        IO.copy_stream(download, temp_sig_file)
        puts "Temp image file path: #{temp_sig_file.path}" if @debug_logging_enabled

        @parameters['xhtml'] = @parameters['xhtml'].gsub(/##Image##/,"#{temp_sig_file.path}")
        puts "XHTML with attachment replacement: \n#{@parameters['xhtml']}"  if @debug_logging_enabled
      end

      # Create a tempfile (the actual full temp file name is determined by the Ruby tempfile process)
      temp_file = Tempfile.new([@filename,'.pdf'])
      # Write the StringIO data to the temp file
      temp_file.write(FlyingRubySaucer::Generator.string_to_pdf(@parameters["xhtml"]));
      # Store the temp_path, which includes the file name, so it can be used later
      temp_path = temp_file.path
      # Close the file, so it can be rereived and read later
      temp_file.close

      # Make the API call to upload the file to Kinetic Core
      request = RestClient::Request.new(
        :method => :post,
        :url => "#{@api_location}/kapps/#{@parameters['kapp_slug']}/forms/#{@parameters['form_slug']}/files",
        :user => @api_username,
        :password => @api_password,
        :payload => {
          :multipart => true,
          :file => File.new(temp_path, 'rb')
        })
      response = request.execute

    rescue RestClient::Exception => e
      error = nil
      response_code = e.response.code

      # Attempt to parse the JSON error message.
      begin
        error = JSON.parse(e.response)
        error_message = error["error"]
        error_key = error["errorKey"] || ""
      rescue Exception
        puts "There was an error parsing the JSON error response" if @debug_logging_enabled
        error_message = e.inspect
      end

      raise if @error_handling == "Raise Error"

    rescue => e
      # Raise the error if instructed to, otherwise will fall through to
      # return an error message.
      error_message = e.inspect

      raise if @error_handling == "Raise Error"
    ensure
      # The unlink deletes the temp file.  This should happen automatically when the handler completes
      # but good practice is to unlink it anyway.
      begin
        temp_file.unlink
        puts "Deleted the stored PDF file: #{temp_path}" if @debug_logging_enabled
        temp_sig_file.unlink
        puts "Deleted the working image file: #{temp_sig_file.path} ...or maybe it was deleted before" if @debug_logging_enabled
      rescue
      end
    end

    # Return (and escape) the results that were defined in the node.xml
    <<-RESULTS
    <results>
      <result name="Handler Error Message">#{escape(error_message)}</result>
      <result name="File">#{escape(response.nil? ? {} : response.body)}</result>
    </results>
    RESULTS
  end

  ##############################################################################
  # General handler utility functions
  ##############################################################################

  # This is a template method that is used to escape results values (returned in
  # execute) that would cause the XML to be invalid.  This method is not
  # necessary if values do not contain character that have special meaning in
  # XML (&, ", <, and >), however it is a good practice to use it for all return
  # variable results in case the value could include one of those characters in
  # the future.  This method can be copied and reused between handlers.
  def escape(string)
    # Globally replace characters based on the ESCAPE_CHARACTERS constant
    string.to_s.gsub(/[&"><]/) { |special| ESCAPE_CHARACTERS[special] } if string
  end
  # This is a ruby constant that is used by the escape method
  ESCAPE_CHARACTERS = {'&'=>'&amp;', '>'=>'&gt;', '<'=>'&lt;', '"' => '&quot;'}
end
