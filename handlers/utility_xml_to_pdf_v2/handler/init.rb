# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class UtilityXmlToPdfV2
  def initialize(input)
    @input_document = REXML::Document.new(input)

    # Parse info values from input XML
    @info_values = {}
    REXML::XPath.each(@input_document, "/handler/infos/info") do |item|
      @info_values[item.attributes["name"]] = item.text.to_s.strip
    end

    # Parse parameters from input XML
    @parameters = {}
    REXML::XPath.each(@input_document, "/handler/parameters/parameter") do |item|
      @parameters[item.attributes["name"]] = item.text.to_s.strip
    end

    @debug_logging_enabled = ["yes","true"].include?(@info_values['enable_debug_logging'].downcase)
    @error_handling = @parameters["error_handling"]

    # Set up API credentials
    @api_location = @info_values["api_location"].chomp("/")
    @api_username = @info_values["api_username"]
    @api_password = @info_values["api_password"]

    @filename = @parameters['filename'].chomp('.pdf')

    # Parse nested attachment mappings from JSON
    # New format:
    # {
    #   "submission_guid": {
    #     "field_name": {
    #       "tag_pattern": "##Tag_*##",
    #       "image_template": "<img src=\"\" width=\"600\" height=\"auto\"></img><br/>"
    #     }
    #   }
    # }
    @attachment_mappings = JSON.parse(@parameters['attachment_mappings'] || '{}')
  end

  def execute
    error_message = nil
    error_key = nil
    response_code = nil
    @temp_files = []

    begin
      # Process each submission and its fields
      @attachment_mappings.each do |submission_id, fields|
        submission_result = fetch_submission(submission_id)
        next unless submission_result

        # Process each attachment field for this submission
        fields.each do |field_name, mapping|
          field_value = submission_result["submission"]["values"][field_name]
          next if field_value.nil?

          tag_pattern = mapping["tag_pattern"]
          files_info = process_attachments(field_value, field_name, submission_id)

          # Process each attachment file for this field
          files_info.each_with_index do |file_info, index|
            temp_file = create_temp_file(file_info)
            @temp_files << temp_file

            # Default image template includes width for PDF compatibility
            image_template = mapping["image_template"] || '<img src="" width="600" height="auto"></img><br/>'
            complete_image = image_template.sub('src=""', "src=\"#{temp_file.path}\"")

            # Look for specific placement tag (e.g., ##Tag_1##)
            specific_tag = tag_pattern.gsub('*', (index + 1).to_s)
            if @parameters['xhtml'].include?(specific_tag)
              @parameters['xhtml'].gsub!(specific_tag, complete_image)
            else
              # If no specific tag found, use wildcard tag and preserve it for next image
              @parameters['xhtml'].gsub!(tag_pattern, "#{complete_image}\n#{tag_pattern}")
            end
          end

          # Clean up any remaining wildcard tags
          @parameters['xhtml'].gsub!(tag_pattern, '')
        end
      end

      pdf_result = generate_and_upload_pdf

    rescue RestClient::Exception => e
      error_message, error_key, response_code = handle_rest_client_error(e)
      raise if @error_handling == "Raise Error"
    rescue => e
      error_message = e.inspect
      raise if @error_handling == "Raise Error"
    ensure
      cleanup_temp_files(@temp_files)
    end

    format_results(error_message, error_key, response_code, pdf_result)
  end

  private

  # Fetches submission data from the API
  def fetch_submission(submission_id)
    submission_api_route = "#{@api_location}/submissions/#{URI.escape(submission_id)}?include=values"
    puts "Route to Submission: #{submission_api_route}" if @debug_logging_enabled

    result = RestClient::Resource.new(
      submission_api_route,
      user: @api_username,
      password: @api_password
    ).get

    JSON.parse(result)
  end

  # Processes attachments for a field, returning array of file info with download URLs
  def process_attachments(field_value, field_name, submission_id)
    files_info = []

    field_value.each_with_index do |file_info, index|
      download_url = fetch_attachment_url(field_name, index, file_info['name'], submission_id)
      file_info["url"] = download_url
      file_info.delete("link")
      files_info << file_info
    end

    files_info
  end

  # Gets download URL for a specific attachment
  def fetch_attachment_url(field_name, index, filename, submission_id)
    attachment_api_route = "#{@api_location}/submissions/#{URI.escape(submission_id)}" +
      "/files/#{URI.escape(field_name)}/#{index}/#{URI.escape(filename)}/url"

    result = RestClient::Resource.new(
      attachment_api_route,
      user: @api_username,
      password: @api_password
    ).get

    JSON.parse(result)['url']
  end

  # Creates a temporary file from attachment URL
  def create_temp_file(file_info)
    extension = File.extname(file_info['name'])
    puts "Using file extension: #{extension} for file: #{file_info['name']}" if @debug_logging_enabled

    response = RestClient::Request.execute(
      method: :get,
      url: file_info['url'],
      user: @api_username,
      password: @api_password,
      raw_response: true      # streams to a Tempfile instead of buffering a String
    )

    temp_file = Tempfile.new(['file', extension])
    temp_file.binmode
    IO.copy_stream(response.file.path, temp_file)
    temp_file.flush
    temp_file.close           # <- important, see below
    puts "Temp image file path: #{temp_file.path}" if @debug_logging_enabled
    temp_file
  end

  # Generates PDF from XHTML and uploads it
  def generate_and_upload_pdf
    temp_pdf = Tempfile.new([@parameters['filename'], '.pdf'])
    temp_pdf.write(FlyingRubySaucer::Generator.string_to_pdf(@parameters["xhtml"]))
    temp_path = temp_pdf.path
    temp_pdf.close
    @temp_files << temp_pdf

    request = RestClient::Request.new(
      method: :post,
      url: "#{@api_location}/kapps/#{@parameters['kapp_slug']}/forms/#{@parameters['form_slug']}/files",
      user: @api_username,
      password: @api_password,
      payload: {
        multipart: true,
        file: File.new(temp_path, 'rb')
      })

    request.execute
  end

  # Handles REST client errors
  def handle_rest_client_error(e)
    response = e.response

    # Connection-level failures (timeouts, DNS, refused) have no response at all
    return [e.inspect, "", nil] if response.nil?

    response_code = response.code
    body = response.respond_to?(:body) ? response.body.to_s : response.to_s

    begin
      parsed = JSON.parse(body)
      if parsed.is_a?(Hash)
        error_message = parsed["error"] || body
        error_key     = parsed["errorKey"].to_s
      else
        error_message = body
        error_key     = ""
      end
    rescue JSON::ParserError => parse_error
      puts "Error parsing JSON error response: #{parse_error.message}" if @debug_logging_enabled
      error_message = "#{e.message} (HTTP #{response_code}): #{body}"
      error_key     = ""
    end

    [error_message, error_key, response_code]
  end

  # Cleans up temporary files
  def cleanup_temp_files(temp_files)
    temp_files.each do |file|
      begin
        file.unlink
        puts "Deleted temp file: #{file.path}" if @debug_logging_enabled
      rescue
      end
    end
  end

  # Formats results for XML output
  def format_results(error_message, error_key, response_code, response)
    <<-RESULTS
    <results>
      <result name="Handler Error Message">#{escape(error_message)}</result>
      <result name="Handler Error Key">#{escape(error_key)}</result>
      <result name="Response Code">#{escape(response_code)}</result>
      <result name="File">#{escape(response.nil? ? {} : response.body)}</result>
    </results>
    RESULTS
  end

  # Escapes special characters for XML output
  def escape(string)
    string.to_s.gsub(/[&"><]/) { |special| ESCAPE_CHARACTERS[special] } if string
  end

  ESCAPE_CHARACTERS = {'&'=>'&amp;', '>'=>'&gt;', '<'=>'&lt;', '"' => '&quot;'}
end
