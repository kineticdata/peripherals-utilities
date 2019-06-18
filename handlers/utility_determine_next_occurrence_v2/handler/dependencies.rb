# Load the Ruby NTLM library unless it has already been loaded.  This
# prevents multiple handlers using the same library from causing problems.
if not defined?(IceCube)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/ice_cube-0.14.0/lib')
  $:.unshift library_path
  # Require the library
  require 'ice_cube'
end

# Validate the the loaded Ruby NTLM library is the library that is expected for
# this handler to execute properly.
if not defined?(IceCube::VERSION)
  raise "The Ice Cube class does not define the expected VERSION constant."
elsif IceCube::VERSION.to_s != '0.14.0'
  raise "Incompatible library version #{IceCube::VERSION} for Ice Cube.  Expecting version 0.14.0."
end
