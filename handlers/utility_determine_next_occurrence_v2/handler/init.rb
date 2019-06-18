# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityDetermineNextOccurrenceV2
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Store the info values in a Hash of info names to values.
    @info_values = {}
    REXML::XPath.each(@input_document,"/handler/infos/info") { |item|
      @info_values[item.attributes['name']] = item.text
    }

    @enable_debug_logging = @info_values['enable_debug_logging'] == 'Yes'

    # Store parameters values in a Hash of parameter names to values.
    @parameters = {}
    REXML::XPath.match(@input_document, '/handler/parameters/parameter').each do |node|
      @parameters[node.attribute('name').value] = node.text.to_s
    end

    # Validate reference type
    recurrence_types = ['minutely','hourly','daily','weekly','monthly','yearly']
    if !recurrence_types.include? @parameters['recurrence_type'].downcase
      raise StandardError, "The value '#{@parameters['recurrence_type']}' doesn't match one of #{recurrence_types.join(",")}"
    end

    # Validate other inputs are entered and configure them for use
    # Timing
    timing_required = ['monthly','yearly']
    if timing_required.include? @parameters['recurrence_type'].downcase && (@parameters['timing'].nil? || @parameters['timing'].empty?)
      raise StandardError, "The 'Timing' field is required for recurrence type '#{@parameters['recurrence_type']}'"
    end
    if ((timing_required.include? @parameters['recurrence_type'].downcase) && (!['relative','absolute'].include? @parameters['timing'].downcase))
      raise StandardError, "The 'Timing' value must be either 'Relative' or 'Absolute'. Current value: '#{@parameters['timing']}'"
    end

    # Interval
    interval_required = ['minutely','hourly','daily','weekly','monthly','yearly']
    if interval_required.include? @parameters['recurrence_type'].downcase && (@parameters['interval'].nil? || @parameters['interval'].empty?)
      raise StandardError, "The 'Interval' field is required for recurrence type '#{@parameters['recurrence_type']}'"
    end

    # Months
    months_required = ['yearly']
    if months_required.include? @parameters['recurrence_type'].downcase
      if (@parameters['months'].nil? || @parameters['months'].empty?)
        raise StandardError, "The 'Months' field is required for recurrence type '#{@parameters['recurrence_type']}'"
      end

      # Set up month inputs
      @months=[]
      if !@parameters['months'].nil? && !@parameters['months'].empty?
        month_conversion = {"january" => :january,"february" => :february,"march" => :march, "april" => :april, "may" => :may, "june" => :june, "july" => :july, "august" => :august, "september" => :september, "october" => :october, "november" => :november, "december" => :december}
        @parameters['months'] = @parameters['months'].split(",").map{|x| x.strip}
        #JSON.parse(@parameters['months']).each { |x|
        @parameters['months'].each { |x|
          @months.push(month_conversion[x.downcase])
        }
      end
      puts "Months (after conversion): #{@months}" if @enable_debug_logging
    end

    # Days of Month
    daysofmonth_required = {'monthly' => {'timing' => 'absolute'}, 'yearly' => {'timing' => 'absolute'}}
    if ((daysofmonth_required.has_key? @parameters['recurrence_type'].downcase) && (daysofmonth_required[@parameters['recurrence_type'].downcase]['timing'] == @parameters['timing'].downcase))
      if (@parameters['days_of_month'].nil? || @parameters['days_of_month'].empty?)
        raise StandardError, "The 'Days of Month' field is required for recurrence type '#{@parameters['recurrence_type']}'"
      end

      # Convert Days of Month array to numberics and convert "Last" to -1
      @days_of_month = []
      if !@parameters['days_of_month'].nil? && !@parameters['days_of_month'].empty?
        @parameters['days_of_month'] = @parameters['days_of_month'].split(",").map{|x| x.strip}
        #JSON.parse(@parameters['days_of_month']).each {|x|
        @parameters['days_of_month'].each {|x|
          if x.downcase==="last"
            @days_of_month.push(-1)
          else
            @days_of_month.push(x.to_i)
          end
        }
      end
      puts "Days of Month (after conversion): #{@days_of_month}" if @enable_debug_logging
    end

    # Weekdays
    weekdays_required = {'weekly' => {'timing' => ''}, 'monthly' => {'timing' => 'relative'}, 'yearly' => {'timing' => 'relative'}}
    if ((weekdays_required.has_key? @parameters['recurrence_type'].downcase) && (weekdays_required[@parameters['recurrence_type'].downcase]['timing'] == @parameters['timing'].downcase))
      if (@parameters['weekdays'].nil? || @parameters['weekdays'].empty?)
        raise StandardError, "The 'Weekdays' field is required for recurrence type '#{@parameters['recurrence_type']}'" if @parameters['recurrence_type'].downcase == "weekly"
        raise StandardError, "The 'Weekdays' field is required for recurrence type '#{@parameters['recurrence_type']}' and timing '#{@parameters['timing']}'" if ['monthly','yearly'].include? @parameters['recurrence_type'].downcase
      end

      @weekdays = []
      if !@parameters['weekdays'].nil? && !@parameters['weekdays'].empty?
        weekday_conversion = {"sunday" => :sunday,"monday" => :monday,"tuesday" => :tuesday, "wednesday" => :wednesday, "thursday" => :thursday, "friday" => :friday, "saturday" => :saturday}
        @parameters['weekdays'] = @parameters['weekdays'].split(",").map{|x| x.strip}
        #JSON.parse(@parameters['weekdays']).each { |x|
        @parameters['weekdays'].each { |x|
          @weekdays.push(weekday_conversion[x.downcase])
        }
      end
      puts "Weekdays (after conversion): #{@weekdays}" if @enable_debug_logging

    end

    # Weekday and Matching Indexes
    weekday_index_required = {'monthly' => {'timing' => 'relative'}, 'yearly' => {'timing' => 'relative'}}
    if ((weekday_index_required.has_key? @parameters['recurrence_type'].downcase) && (weekday_index_required[@parameters['recurrence_type'].downcase]['timing'] == @parameters['timing'].downcase))

      if ((@weekdays.include? :sunday    && (@parameters['sunday_index'].nil?    || @parameters['sunday_index'].empty?)) ||
        (@weekdays.include? :monday      && (@parameters['monday_index'].nil?    || @parameters['monday_index'].empty?)) ||
        (@weekdays.include? :tuesday     && (@parameters['tuesday_index'].nil?   || @parameters['tuesday_index'].empty?)) ||
        (@weekdays.include? :wednesday   && (@parameters['wednesday_index'].nil? || @parameters['wednesday_index'].empty?)) ||
        (@weekdays.include? :thursday    && (@parameters['thursday_index'].nil?  || @parameters['thursday_index'].empty?)) ||
        (@weekdays.include? :friday      && (@parameters['friday_index'].nil?    || @parameters['friday_index'].empty?)) ||
        (@weekdays.include? :saturday    && (@parameters['saturday_index'].nil?  || @parameters['saturday_index'].empty?)))
        raise StandardError, "A corresponding weekday index is required for each weekday identified for recurrence type '#{@parameters['recurrence_type']}' and timing '#{@parameters['timing']}'"
      end

      #set up day inputs with index
      @weekdays_with_index = []
      if @weekdays.include? :sunday
        @weekdays_with_index.push([:sunday, weekday_index("sunday")])
      end
      if @weekdays.include? :monday
        @weekdays_with_index.push([:monday, weekday_index("monday")])
      end
      if @weekdays.include? :tuesday
        @weekdays_with_index.push([:tuesday, weekday_index("tuesday")])
      end
      if @weekdays.include? :wednesday
        @weekdays_with_index.push([:wednesday, weekday_index("wednesday")])
      end
      if @weekdays.include? :thursday
        @weekdays_with_index.push([:thursday, weekday_index("thursday")])
      end
      if @weekdays.include? :friday
        @weekdays_with_index.push([:friday, weekday_index("friday")])
      end
      if @weekdays.include? :saturday
        @weekdays_with_index.push([:saturday, weekday_index("saturday")])
      end
      puts "Weekdays with Index (after conversion): #{@weekdays_with_index}" if @enable_debug_logging
      @weekdays_with_index_as_param = Hash[*@weekdays_with_index.flatten(1)]
      puts "Weekdays with Index as parameters: #{@weekdays_with_index_as_param}" if @enable_debug_logging
    end

  end

  def execute()

    rule = case @parameters['recurrence_type'].downcase
      when "minutely"
        IceCube::Rule.minutely(@parameters['interval'].to_i)
      when "hourly"
        IceCube::Rule.hourly(@parameters['interval'].to_i)
      when "daily"
        IceCube::Rule.daily(@parameters['interval'].to_i)
      when "weekly"
        IceCube::Rule.weekly(@parameters['interval'].to_i).day(*@weekdays)
      when "monthly"
        if @parameters['timing'].downcase == "absolute"
          IceCube::Rule.monthly(@parameters['interval'].to_i).day_of_month(*@days_of_month)
        else
          IceCube::Rule.monthly(@parameters['interval'].to_i).day_of_week(@weekdays_with_index_as_param)
        end
      when "yearly"
        if @parameters['timing'].downcase == "absolute"
          IceCube::Rule.yearly(@parameters['interval'].to_i).month_of_year(*@months).day_of_month(*@days_of_month)
        else
          IceCube::Rule.yearly(@parameters['interval'].to_i).month_of_year(*@months).day_of_week(@weekdays_with_index_as_param)
        end
    end

    # convert start time parameter to date/time
    start_time = Time.parse(@parameters['start'])

    schedule = IceCube::Schedule.new(start_time)
    schedule.add_recurrence_rule rule

    next_occurrence = schedule.next_occurrence

    # return next occurrence
     <<-RESULTS
    <results>
      <result name="next_occurrence">#{next_occurrence.to_s}</result>
    </results>
    RESULTS
  end


  def weekday_index(day)
    index_array = []
    #@parameters[day+"_index"] = @parameters[day+"_index"].split(",").map{|x| x.strip}
    day_index = @parameters[day+"_index"].split(",").map{|x| x.strip}
    #JSON.parse(@parameters[day+"_index"]).each{ |x|
    #@parameters[day+"_index"].each{ |x|
    day_index.each{ |x|
      case x.downcase
      when "all"
        index_array.push(1,2,3,4,-1)
      when "first"
        index_array.push(1)
      when "second"
        index_array.push(2)
      when "third"
        index_array.push(3)
      when "fourth"
        index_array.push(4)
      when "last"
        index_array.push(-1)
      end
    }
    puts "#{day}_index array: #{index_array}" if @enable_debug_logging
    return index_array
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
