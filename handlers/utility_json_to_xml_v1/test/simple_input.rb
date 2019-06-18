{
  'parameters' => {
    'json' => <<JSON
{
  "questions" : [
    {
      "general fields": {
        "text" : "Name",
        "type" : "Free Text"
    },
      "other fields": {
        "size" : 30,
        "rows" : 2
      }
    },
    {
      "general fields": {
        "text" : "Email Address",
        "type" : "Email"
    },
      "other fields": {
        "size" : 50
      }
    },
    {
      "general fields": {
        "text" : "Date of Birth",
        "type" : "Date"
    },
      "other fields": {
        "hide date field" : false,
        "show calendar" : true
      }
    }
  ]
}
JSON
  }
}