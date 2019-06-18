require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityJsonToResultsV3
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)
    
    # Retrieve all of the handler parameters and store them in a hash attribute
    # named @parameters.
    @parameters = {}
    REXML::XPath.match(@input_document, 'handler/parameters/parameter').each do |node|
      @parameters[node.attribute('name').value] = node.text.to_s
    end
  end
  
  def execute()
    
    parsed = JSON.parse(@parameters['json'])

    # Sort json by key value
    parsed = parsed.sort_by {|name,value| name}

    # For each value in the hash, build an xml string of results.
    xmlstring = "<results>\n"
    parsed.each {|k|
      name = k[0]
      value = k[1]
      xmlstring << "<result name=\"#{escape(name)}\">#{escape(value)}</result>\n"
    }
    xmlstring << "</results>"

    puts xmlstring;

    <<-RESULTS
    #{xmlstring}
    RESULTS
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
