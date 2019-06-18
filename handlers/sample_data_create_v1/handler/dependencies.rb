require 'rexml/document'
require 'csv'

# If the Kinetic Task version is under 4, load the openssl and json libraries
# because they are not included in the ruby version
if KineticTask::VERSION.split(".").first.to_i < 4
  # load dependenices

  # Load the ruby json library unless it has already been loaded.  This prevents 
  # multiple handlers using the same library from causing problems.
  if not defined?(JSON)
    # Calculate the location of this file
    handler_path = File.expand_path(File.dirname(__FILE__))
    # Calculate the location of our library and add it to the Ruby load path
    library_path = File.join(handler_path, 'vendor/json-1.4.6-java/lib')
    $:.unshift library_path
    # Require the library
    require 'json'
  end

  # Validate the the loaded JSON library is the library that is expected for
  # this handler to execute properly.
  if not defined?(JSON::VERSION)
    raise "The JSON class does not define the expected VERSION constant."
  elsif JSON::VERSION.to_s != '1.4.6'
    raise "Incompatible library version #{JSON::VERSION} for JSON.  Expecting version 1.4.6."
  end
end

# Load the ruby concurrent/map library unless it has already been loaded.  This prevents 
# multiple handlers using the same library from causing problems.  concurrent/map is a
# dependency file for Faker
if not defined?(concurrent/map)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/concurrent-ruby-1.1.4/lib')
  $:.unshift library_path
  # Require the library
  require 'concurrent/map'
end

#TODO: modify the concurrent vendor file to have a version.

# Load the ruby i18n library unless it has already been loaded.  This prevents 
# multiple handlers using the same library from causing problems.  i18n is a
# dependency file for Faker
if not defined?(I18n)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/i18n-1.4.0/lib')
  $:.unshift library_path
  # Require the library
  require 'i18n'
end

# Validate the the loaded I18n library is the library that is expected for
# this handler to execute properly.
if not defined?(I18n::VERSION)
  raise "The I18n class does not define the expected VERSION constant."
elsif I18n::VERSION.to_s != '1.4.0'
  raise "Incompatible library version #{I18n::VERSION} for I18n.  Expecting version 1.4.0."
end

# Load the ruby faker library unless it has already been loaded.  This prevents 
# multiple handlers using the same library from causing problems.
if not defined?(Faker)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/faker-1.9.1/lib')
  $:.unshift library_path
  # Require the library
  require 'faker'
end

# Validate the the loaded JSON library is the library that is expected for
# this handler to execute properly.
if not defined?(Faker::VERSION)
  raise "The Faker class does not define the expected VERSION constant."
elsif Faker::VERSION.to_s != '1.9.1'
  raise "Incompatible library version #{Faker::VERSION} for Faker.  Expecting version 1.9.1."
end
