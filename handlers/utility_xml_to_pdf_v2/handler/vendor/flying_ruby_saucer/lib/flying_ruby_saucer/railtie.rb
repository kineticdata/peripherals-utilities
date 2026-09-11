require 'rails'

class FlyingRubySaucer::Railtie < Rails::Railtie
  # Register as renderer when ActionController has been initialized
  initializer "flying_ruby_saucer_railtie.configure_rails_initialization",
    :after => "action_controller.compile_config_methods" do

    ActionController::Renderers.add :pdf do |template, options|
      # Reuse the render semantics to get a string from the
      # template and options
      string = render_to_string template, options  
      send_data FlyingRubySaucer::Generator.string_to_pdf(string), :type => Mime::PDF
    end
  end
end