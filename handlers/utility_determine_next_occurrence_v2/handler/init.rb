# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), 'dependencies'))

class UtilityDetermineNextOccurrenceV2
  # Supported values for the 'Recurrence Type' parameter
  RECURRENCE_TYPES = ['minutely', 'hourly', 'daily', 'weekly', 'monthly', 'yearly']

  # Supported values for the 'Timing' parameter (monthly and yearly only)
  TIMINGS = ['relative', 'absolute']

  # Month names accepted by the 'Months' parameter, mapped to IceCube symbols
  MONTHS = {
    'january' => :january, 'february' => :february, 'march' => :march,
    'april' => :april, 'may' => :may, 'june' => :june, 'july' => :july,
    'august' => :august, 'september' => :september, 'october' => :october,
    'november' => :november, 'december' => :december
  }

  # Weekday names accepted by the 'Weekdays' parameter, mapped to IceCube
  # symbols.  The keys also name the matching '<weekday>_index' parameters.
  WEEKDAYS = {
    'sunday' => :sunday, 'monday' => :monday, 'tuesday' => :tuesday,
    'wednesday' => :wednesday, 'thursday' => :thursday, 'friday' => :friday,
    'saturday' => :saturday
  }

  # Values accepted by the '<weekday>_index' parameters, mapped to the
  # occurrence numbers IceCube expects (-1 is the last occurrence in the month).
  # 'All' uses 1-4 plus last, which also covers a fifth occurrence when a month
  # has one.
  WEEKDAY_INDEXES = {
    'all' => [1, 2, 3, 4, -1], 'first' => [1], 'second' => [2],
    'third' => [3], 'fourth' => [4], 'last' => [-1]
  }

  # How far ahead to look for an occurrence before deciding the schedule can
  # never occur.  Long enough for any real schedule, including February 29
  # with a multi-year interval.
  SEARCH_YEARS = 100

  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Store the info values in a Hash of info names to values.
    @info_values = {}
    REXML::XPath.each(@input_document,"/handler/infos/info") { |item|
      @info_values[item.attributes['name']] = item.text
    }

    @enable_debug_logging = @info_values['enable_debug_logging'] == 'Yes'

    # Store parameters values in a Hash of parameter names to values.  Values
    # are stripped so stray whitespace from the tree doesn't fail validation.
    @parameters = {}
    REXML::XPath.match(@input_document, '/handler/parameters/parameter').each do |node|
      @parameters[node.attribute('name').value] = node.text.to_s.strip
    end
    puts "Parameters: #{@parameters.inspect}" if @enable_debug_logging

    # Recurrence Type (always required)
    @recurrence_type = @parameters['recurrence_type'].downcase
    if !RECURRENCE_TYPES.include?(@recurrence_type)
      raise StandardError, "The 'Recurrence Type' value '#{@parameters['recurrence_type']}' doesn't match one of #{RECURRENCE_TYPES.join(', ')}"
    end

    # Start and Interval (always required)
    @start_time = parse_start_time(@parameters['start'])
    @interval = parse_interval(@parameters['interval'])
    puts "Start time: #{@start_time}, Interval: #{@interval}" if @enable_debug_logging

    # Timing (monthly and yearly only)
    if ['monthly', 'yearly'].include?(@recurrence_type)
      @timing = @parameters['timing'].downcase
      if !TIMINGS.include?(@timing)
        raise StandardError, "The 'Timing' value must be either 'Relative' or 'Absolute' for recurrence type '#{@parameters['recurrence_type']}'. Current value: '#{@parameters['timing']}'"
      end
    end

    # Months (yearly only)
    if @recurrence_type == 'yearly'
      @months = parse_list('months', 'Months', MONTHS)
      puts "Months (after conversion): #{@months}" if @enable_debug_logging
    end

    # Days of Month (monthly/yearly with absolute timing)
    if @timing == 'absolute'
      @days_of_month = parse_days_of_month
      puts "Days of Month (after conversion): #{@days_of_month}" if @enable_debug_logging
    end

    # Weekdays (weekly, or monthly/yearly with relative timing).  Weekly
    # ignores Timing, so a leftover Timing value can't hide the weekdays.
    if @recurrence_type == 'weekly' || @timing == 'relative'
      @weekdays = parse_list('weekdays', 'Weekdays', WEEKDAYS)
      puts "Weekdays (after conversion): #{@weekdays}" if @enable_debug_logging
    end

    # Weekday indexes (monthly/yearly with relative timing), built into the
    # Hash that IceCube's day_of_week expects, e.g. {:monday => [1, -1]}
    if @timing == 'relative'
      @weekdays_with_index = {}
      @weekdays.each do |weekday|
        @weekdays_with_index[weekday] = parse_weekday_index(weekday.to_s)
      end
      puts "Weekdays with Index: #{@weekdays_with_index}" if @enable_debug_logging
    end
  end

  def execute()
    # Build the recurrence rule for the requested type and timing
    rule = case @recurrence_type
      when "minutely"
        IceCube::Rule.minutely(@interval)
      when "hourly"
        IceCube::Rule.hourly(@interval)
      when "daily"
        IceCube::Rule.daily(@interval)
      when "weekly"
        IceCube::Rule.weekly(@interval).day(*@weekdays)
      when "monthly"
        if @timing == "absolute"
          IceCube::Rule.monthly(@interval).day_of_month(*@days_of_month)
        else
          IceCube::Rule.monthly(@interval).day_of_week(@weekdays_with_index)
        end
      when "yearly"
        if @timing == "absolute"
          IceCube::Rule.yearly(@interval).month_of_year(*@months).day_of_month(*@days_of_month)
        else
          IceCube::Rule.yearly(@interval).month_of_year(*@months).day_of_week(@weekdays_with_index)
        end
    end
    # Describe the rule in iCalendar form for logs and errors.  IceCube's
    # to_s needs ActiveSupport's i18n, which may not be loaded in Task.
    rule_description = rule.to_ical
    puts "Rule: #{rule_description}" if @enable_debug_logging

    # Anchor the schedule at the start time
    schedule = IceCube::Schedule.new(@start_time)
    schedule.add_recurrence_rule rule

    # IceCube searches forever for a rule that can never match (ex: February
    # 30, or the 31st every 12 months starting in April), which would hang the
    # Task engine thread.  Confirm an occurrence exists within a bounded window
    # first, since that search stops at its closing time.
    search_from = [Time.now, @start_time].max
    search_until = search_from + (SEARCH_YEARS * 366 * 24 * 60 * 60)
    if !schedule.occurs_between?(search_from, search_until)
      raise StandardError, "No occurrence of the schedule '#{rule_description}' starting #{@start_time} could be found within #{SEARCH_YEARS} years. Check that the Months and Days of Month can occur together for the Interval given."
    end

    # Find the first occurrence after now
    next_occurrence = schedule.next_occurrence

    # Return the next occurrence in UTC (ex: 2018-03-02 22:00:00 UTC)
    next_occurrence_utc = next_occurrence.to_time.getutc.to_s
    puts "Next occurrence: #{next_occurrence_utc}" if @enable_debug_logging

    <<-RESULTS
    <results>
      <result name="next_occurrence">#{escape(next_occurrence_utc)}</result>
    </results>
    RESULTS
  end

  # Parses the Start parameter.  Ruby's Time.parse reads a value with no offset
  # as server local time, but this parameter is documented as UTC, so default
  # to UTC when no offset is given.  A given offset is kept so day boundaries
  # are calculated in that offset.
  def parse_start_time(value)
    if value.empty?
      raise StandardError, "The 'Start' field is required"
    end

    begin
      if Date._parse(value)[:offset].nil?
        Time.parse("#{value} UTC")
      else
        Time.parse(value)
      end
    rescue ArgumentError
      raise StandardError, "The 'Start' value '#{value}' is not a parsable date/time (ex: 2017-05-17T14:00:00+00:00)"
    end
  end

  # Parses the Interval parameter, which must be a whole number of 1 or more
  def parse_interval(value)
    if value !~ /\A\d+\z/ || value.to_i < 1
      raise StandardError, "The 'Interval' value must be a whole number of 1 or more. Current value: '#{value}'"
    end
    value.to_i
  end

  # Splits a comma separated parameter and converts each entry using the given
  # Hash of accepted (lowercase) values.  Blank entries, such as from a
  # trailing comma, are skipped and duplicates are removed.
  def parse_list(parameter_name, label, accepted_values)
    entries = split_list(@parameters[parameter_name])
    if entries.empty?
      raise StandardError, "The '#{label}' field is required for recurrence type '#{@parameters['recurrence_type']}'#{timing_description}"
    end

    entries.map { |entry|
      accepted_values.fetch(entry.downcase) {
        raise StandardError, "The '#{label}' value '#{entry}' is not valid. Valid values are: #{accepted_values.keys.map(&:capitalize).join(', ')}"
      }
    }.uniq
  end

  # Converts the Days of Month parameter to the numbers IceCube expects, with
  # 'Last' becoming -1
  def parse_days_of_month
    entries = split_list(@parameters['days_of_month'])
    if entries.empty?
      raise StandardError, "The 'Days of Month' field is required for recurrence type '#{@parameters['recurrence_type']}'#{timing_description}"
    end

    entries.map { |entry|
      if entry.downcase == 'last'
        -1
      elsif entry =~ /\A\d+\z/ && (1..31).include?(entry.to_i)
        entry.to_i
      else
        raise StandardError, "The 'Days of Month' value '#{entry}' is not valid. Valid values are 1 through 31 and 'Last'"
      end
    }.uniq
  end

  # Converts a '<weekday>_index' parameter to the occurrence numbers for that
  # weekday, ex: 'First, Last' becomes [1, -1]
  def parse_weekday_index(day)
    label = "#{day.capitalize} Index"
    entries = split_list(@parameters["#{day}_index"])
    if entries.empty?
      raise StandardError, "The '#{label}' field is required when '#{day.capitalize}' is one of the Weekdays for recurrence type '#{@parameters['recurrence_type']}'#{timing_description}"
    end

    index_array = entries.flat_map { |entry|
      WEEKDAY_INDEXES.fetch(entry.downcase) {
        raise StandardError, "The '#{label}' value '#{entry}' is not valid. Valid values are: #{WEEKDAY_INDEXES.keys.map(&:capitalize).join(', ')}"
      }
    }.uniq
    puts "#{day}_index array: #{index_array}" if @enable_debug_logging
    index_array
  end

  # Splits a comma separated value into stripped, non-blank entries
  def split_list(value)
    value.to_s.split(",").map { |entry| entry.strip }.reject { |entry| entry.empty? }
  end

  # Describes the timing for validation messages, when timing applies
  def timing_description
    @timing.nil? ? "" : " and timing '#{@parameters['timing']}'"
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
