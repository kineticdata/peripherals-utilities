# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityCsvToJsonV1
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

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
    csv_structure = CSV.parse(@parameters['csv'])
    json = convert_csv_to_json(csv_structure)
    
    <<-RESULTS
    <results>
      <result name="JSON">#{escape(json)}</result>
    </results>
    RESULTS
  end
  
  def convert_csv_to_json(csv)
    # It is assumed that there is a header line in the CSV, because a header
    # is needed for proper JSON format
    header = csv[0]
    check_header(header)
    csv.shift()

    finalArray = []
    csv.each_with_index do |row, i|
      if row.length > header.length
        raise StandardError, "Invalid CSV: Row #{i+2} contains too many values"
      end

      rowHash = {}
      for i in 0..row.length
        if row[i] != nil then
          rowHash.merge!({"#{header[i]}" => "#{row[i]}"})
        end
      end  
      finalArray.push(rowHash)
    end
    {"table" => finalArray}.to_json
  end

  def check_header(header)
    if header.include?(nil)
      raise StandardError, "Invalid CSV: Header contains a nil value"
    end

    if header.uniq.length != header.length
      raise StandardError, "Invalid CSV: Header contains duplicates"
    end
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

end