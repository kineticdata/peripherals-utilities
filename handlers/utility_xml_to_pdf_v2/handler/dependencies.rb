require 'rexml/document'
require 'json'
require 'tempfile'
require 'open-uri'
require 'open3'

handler_path = File.expand_path(File.dirname(__FILE__))

# Load the ruby Mime Types library unless it has already been loaded.  This prevents
# multiple handlers using the same library from causing problems.
if not defined?(MIME)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/mime-types-1.19/lib/')
  $:.unshift library_path
  # Require the library
  require 'mime/types'
end

# Validate the the loaded Mime Types library is the library that is expected for
# this handler to execute properly.
if not defined?(MIME::Types::VERSION)
  raise "The Mime class does not define the expected VERSION constant."
elsif MIME::Types::VERSION != '1.19'
  raise "Incompatible library version #{MIME::Types::VERSION} for Mime Types.  Expecting version 1.19."
end


# Load the ruby rest-client library (used by the Octokit library) unless
# it has already been loaded.  This prevents multiple handlers using the same
# library from causing problems.
if not defined?(RestClient)
  # Calculate the location of this file
  handler_path = File.expand_path(File.dirname(__FILE__))
  # Calculate the location of our library and add it to the Ruby load path
  library_path = File.join(handler_path, 'vendor/rest-client-1.6.7/lib')
  $:.unshift library_path
  # Require the library
  require 'rest-client'
end

# Validate the the loaded rest-client library is the library that is expected for
# this handler to execute properly.
if not defined?(RestClient.version)
  raise "The RestClient class does not define the expected VERSION constant."
elsif RestClient.version.to_s != '1.6.7'
  raise "Incompatible library version #{RestClient.version} for rest-client.  Expecting version 1.6.7."
end

# Load the ruby flying_ruby_saucer library unless it has already been loaded.  This
# prevents multiple handlers using the same library from causing problems.
if not defined?(FlyingRubySaucer)
  library_path = File.join(handler_path, "vendor/flying_ruby_saucer/lib")
  $:.unshift library_path
  require "flying_ruby_saucer"
end
