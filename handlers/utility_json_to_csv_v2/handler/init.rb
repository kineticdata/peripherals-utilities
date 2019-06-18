# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityJsonToCsvV2
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Determine if debug logging is enabled.
    @debug_logging_enabled = get_info_value(@input_document, 'enable_debug_logging') == 'Yes'
    puts("Logging enabled.") if @debug_logging_enabled

    # Retrieve all of the handler parameters and store them in a hash attribute
    # named @parameters.
    @parameters = {}
    REXML::XPath.match(@input_document, '/handler/parameters/parameter').each do |node|
      # Associate the attribute name to the String value (stripping leading and
      # trailing whitespace)
      @parameters[node.attribute('name').value] = node.text.to_s.strip
    end
  end

  def execute()
    json_structure = JSON.parse(@parameters['json'])
    csv = convert_json_to_csv(json_structure)
    puts("csv: #{csv}") if @debug_logging_enabled
    <<-RESULTS
    <results>
      <result name="CSV">#{escape(csv)}</result>
    </results>
    RESULTS
  end

  def convert_json_to_csv(json)
    # We assume that the first value of the JSON structure is the table of data.
    rows = json.values.first

    # Build the headers by appending all of the keys in each row of data.
    headers = []
    rows.each do |row|
      headers = headers | row.keys
    end
    headers.sort!

    # Build the CSV string from the headers Array and iterating through each row
    # of the JSON structure.
    csv_string = CSV.generate do |csv|
	    csv << headers
		rows.each do |row|
		  data = headers.collect {|header| row[header]}
		  #puts "this data="
		  #puts data.inspect
		  csv << data
		end
    end
	csv_string
  end

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

    # This is a sample helper method that illustrates one method for retrieving
  # values from the input document.  As long as your node.xml document follows
  # a consistent format, these type of methods can be copied and reused between
  # handlers.
  def get_info_value(document, name)
    # Retrieve the XML node representing the desired info value
    info_element = REXML::XPath.first(document, "/handler/infos/info[@name='#{name}']")
    # If the desired element is nil, return nil; otherwise return the text value of the element
    info_element.nil? ? nil : info_element.text
  end

end