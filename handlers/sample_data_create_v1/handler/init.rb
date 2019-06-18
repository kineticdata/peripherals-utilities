# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class SampleDataCreateV1
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
    elements = @parameters['field'].split('.', 2)
    error_handling = @parameters['error_handling']
    output = ''
    
    begin
      className = elements[0]
      method = elements[1]
      if method.include? "("
        index = method.index('(')
        arguments = method[(index + 1)..-2]
        arguments = arguments.parse_csv
        arguments = arguments.collect{|ele| 
          ele = ele.strip
          ele = ele.match(/\A[-+]?\d+\z/) ? ele.to_i : ele
          if ele.is_a? String
            ele = ele.match(/\d*[,]?\d*[.]\d++(?!%)/) ? ele.to_f : ele 
          end
          if ele.is_a? String
            ele = ele.match(/^(true)$/) ? true : ele
          end
          if ele.is_a? String 
            ele = ele.match(/^(false)$/) ? false : ele
          end
          ele
        }
        method = method[0..index - 1]
      end

      case className
      when "Address", "Ancient", "App", "Appliance", "AquaTeenHungerForce", "Artist", "Avatar",
        "BackToTheFuture", "Bank", "Beer", "Bitcoin", "BojackHorseman", "Book", "Boolean", "BossaNova",
        "BreakingBad", "Business", "Cannabis", "Cat", "ChuckNorris", "Code", "Coffee", "Color", "Commerce",
        "Community", "Company", "Compass", "Crypto", "Currency", "Date", "Demographic", "Dessert", "Device",
        "Dog", "Dota", "DrWho", "DragonBall", "DumbAndDumber", "Dune", "Educator", "ElderScrolls",
        "ElectricalComponents", "Esport", "Ethereum", "Fallout", "FamilyGuy", "FamousLastWords", "File",
        "Fillmurray", "Finance", "Food", "Football", "Friends", "FunnyName", "GameOfThrones", "Gender",
        "GreekPhilosophers", "Hacker", "HarryPotter", "HeyArnold", "Hipster", "HitchhikersGuideToTheGalaxy",
        "Hobbit", "HowIMetYourMother", "IDNumber", "Internet", "Invoice", "Job", "Kpop", "LeagueOfLegends",
        "Lebowski", "LordOfTheRings", "Lorem", "LoremFlickr", "LoremPixel", "Lovecraft", "Markdown", "Matz",
        "Measurement", "MichaelScott", "Military", "MostInterestingManInTheWorld", "Movie", "Music", "Myst",
        "Name", "Nation", "NatoPhoneticAlphabet", "NewGirl", "Number", "Omniauth", "OnePiece", "Overwatch",
        "ParksAndRec", "PhoneNumber", "Placeholdit", "Pokemon", "PrincessBride", "ProgrammingLanguage",
        "RickAndMorty", "Robin", "RockBand", "RuPaul", "Science", "Seinfeld", "SiliconValley", "Simpsons",
        "SingularSiegler", "SlackEmoji", "Source", "Space", "StarTrek", "StarWars", "StrangerThings", "String",
        "Stripe", "Superhero", "SwordArtOnline", "Team", "TheFreshPrinceOfBelAir", "TheITCrowd", "TheThickOfIt",
        "Time", "TwinPeaks", "Twitter", "Types", "UmphreysMcgee", "University", "VForVendetta", "Vehicle",
        "VentureBros", "Witcher", "WorldCup", "WorldOfWarcraft", "Yoda", "Zelda"

        classConst = Faker.const_get(className)
        if classConst.methods(false).include?(method.to_sym)
          output = classConst.public_send(method, *arguments)
        end
      end
    rescue Exception => error
      error_message = error.inspect
      raise error if error_handling == "Rasie Error"
    end

    <<-RESULTS
    <results>
      <result name="Handler Error Message">#{escape(error_message)}</result>
      <result name="output">#{escape(output)}</result>
    </results>
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