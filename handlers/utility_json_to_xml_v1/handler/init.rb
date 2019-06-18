# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityJsonToXmlV1
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)
    # Initialize the REXML formatter.  Also set the compact attribute to true, this
    # will result in no new lines for text within XML elements when printed.
    @formatter = REXML::Formatters::Pretty.new
    @formatter.compact = true

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
    xml = convert_json_to_xml(json_structure)
    string = @formatter.write(xml, "")
    
    <<-RESULTS
    <results>
      <result name="XML">#{escape(string)}</result>
    </results>
    RESULTS
  end

  # This method converts a Ruby JSON Hash to a REXML::Element object.  The REXML::Element
  # that is returned is the root node of the XML structure and all of the resulting
  # XML data will be nested within that single element.
  def convert_json_to_xml(data, label=nil)
    if data.is_a?(Hash)
      element = REXML::Element.new("node")
      element.add_attribute("type", "Object")
      element.add_attribute("name", label) if label
      data.keys.each do |key|
        element.add_element(convert_json_to_xml(data[key], key))
      end
      element
    elsif data.is_a?(Array)
      element = REXML::Element.new("node")
      element.add_attribute("type", "Array")
      element.add_attribute("name", label) if label
      data.each do |child_data|
        element.add_element(convert_json_to_xml(child_data))
      end
      element
    else
      element = REXML::Element.new("node")
      element.add_attribute("type", data.class.name)
      element.add_attribute("name", label) if label
      element.add_text(data.to_s)
      element
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