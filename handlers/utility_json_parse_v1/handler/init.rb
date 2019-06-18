# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityJsonParseV1
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
    json = JSON.parse(@parameters['json'])
    path = @parameters['path'].split("/")
    data = [parseJson(json, path, 0)].flatten

    if @parameters['unique'] == 'Yes'
      data.uniq!
    end

    xml = "<items>"
    data.each do |item|
      xml << "<item>#{item}</item>"
    end
    xml << "</items>"

    <<-RESULTS
    <results>
      <result name="Items">#{escape(xml)}</result>
    </results>
    RESULTS
  end

  # This is a recursive function that parses a JSON object with a given path.
  def parseJson(json, path, depth)
    # If the current JSON structure is an Array, we will simply iterate over it
    # calling and collecting the results of a recursive call.  Note that we do
    # not increment the depth when parsing an array.
    if json.is_a?(Array)
      json.collect {|item| parseJson(item, path, depth)}
    # If the current JSON structure is a Hash, we will either make a recursive
    # call and increment the depth or we will return the String at the end of
    # the path.
    elsif json.is_a?(Hash)
      # Check that the current JSON structure has a key to match the current
      # path variable.  If not, the path does not match the JSON structure so we
      # raise an exception.
      key = path[depth]
      unless json.keys.member?(key)
        raise "The given path does not match the JSON structure."
      end
      # If the depth is not the index of the last element in the path array we
      # will make a recursive call and increment the depth.  We use the current
      # path variable to pass a new structure by accessing a specific value in
      # the Hash.
      if depth < path.size-1
        parseJson(json[key], path, depth+1)
      # If the depth is the last index in the path array we will return the
      # resulting value.
      else
        # If the resulting value would not be a String we raise an exception.
        # This probably means that the path was not long enough to reach the
        # desired data.
        unless json[key].is_a?(String)
          raise "Reached a non-String value after the JSON was completely parsed with the given path."
        end
        json[key]
      end
    # If the JSON structure is not an Array or Hash we have reached an illegal
    # state.  It is probably due to the path being too long and the parsing
    # reached a String unexpectedly.
    else
      raise "Reached a String value before the JSON was completely parsed with the given path."
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