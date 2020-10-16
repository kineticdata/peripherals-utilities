require 'java'

handler_path = File.expand_path(File.dirname(__FILE__))

def java_class_exists(name)
  begin
    java.lang.Class.forName(name)
    return true
  rescue
    return false
  end
end

if !java_class_exists("com.jayway.jsonpath.JsonPath")
  require File.join(handler_path, "vendor", "accessors-smart-1.2.jar")
  require File.join(handler_path, "vendor", "asm-5.0.4.jar")
  require File.join(handler_path, "vendor", "hamcrest-core-1.3.jar")
  require File.join(handler_path, "vendor", "hamcrest-library-1.3.jar")
  require File.join(handler_path, "vendor", "json-path-2.4.0.jar")
  require File.join(handler_path, "vendor", "json-path-assert-2.4.0.jar")
  require File.join(handler_path, "vendor", "json-smart-2.3.jar")
  require File.join(handler_path, "vendor", "slf4j-api-1.7.25.jar")
end

import com.jayway.jsonpath.JsonPath