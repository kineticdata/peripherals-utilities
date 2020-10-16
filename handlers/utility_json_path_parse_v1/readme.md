# Json Path Parse

When provided with a JsonPath expression and JSON will return JSON.

## Parameters
 * [Error Handling]
  Determine what to return if an error is encountered.
 * [Json Path]
  The Json Path expression. 
 * [Json]
  The Json Object that the expression should be run against.

## Sample Configuration
Error Handling:           Error Message
Json Path: 					      $.foo
Json:                     {"foo": 1}

## Results
[Handler Error Message]
  Error message if an error was encountered and Error Handling is set to "Error Message".
[output]
  Resulting JSON after expression has been applied.

## Notes
* This handler uses [Jayway Json Path](https://github.com/json-path/JsonPath).  Refer to documentation on pattern for building Json Path expressions.
* Tested on Task v4.2.0