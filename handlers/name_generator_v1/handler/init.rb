class NameGeneratorV1
  def initialize(input)
  end

# Borrowed from Haikunator - but simplified for my needs
    def adjectives
      %w(
        autumn hidden bitter misty silent empty dry dark summer
        icy delicate quiet white cool spring winter patient
        twilight dawn crimson wispy weathered blue billowing
        broken cold damp falling frosty green long late lingering
        bold little morning muddy old red rough still small
        sparkling throbbing shy wandering withered wild black
        young holy solitary fragrant aged snowy proud floral
        restless divine polished ancient purple lively nameless
      )
    end

    def nouns
      %w(
        waterfall river breeze moon rain wind sea morning
        snow lake sunset pine shadow leaf dawn glitter forest
        hill cloud meadow sun glade bird brook butterfly
        bush dew dust field fire flower firefly feather grass
        haze mountain night pond darkness snowflake silence
        sound sky shape surf thunder violet water wildflower
        wave water resonance sun wood dream cherry tree fog
        frost voice paper frog smoke star
      )
    end


  def build
    x = adjectives[rand(adjectives.count)]
    y = nouns[rand(nouns.count)]
    z = rand(100)
    return  "#{x}-#{y}-#{z}"
  end

  def execute
    # Return the results
    <<-RESULTS
    <results>
      <result name="Name">#{build}</result>
    </results>
    RESULTS
  end

end