# Generic File Upload CE Attachment
    This handler is used to copy an attachment from a submission to a third-party using a generic api with token validation in it's url.

## Info Values
    [Api Server]
       Kinetic platform api server: http://server:port/kinetic
    [Api Username]   
       Kinetic platform user username
    [Api Password]   
       Kinetic platform user password 
    [Space Slug]  
      Slug of the space that contains the attached file to copy
    [Destination Api]
      The base url for your third-party destination
    [Destination Token Name]
      The token name to be attached to the url
    [Destination Token Value]
      the token value to be attached to the url  

## Parameters
    [Error Handling]
        How to handle error conditions in the handler: raise the error, or return error message
    [Space Slug]
        Slug of the space that contains the attached file to copy
    [From Submission Id]
        Submission Id that contains the attached file to copy.
    [From Form Field Name]
        Name of the file attachment field on the Kinetic Request CE form to copy from
    [Attachment Index]
        The index of the aatachment you want to copy 
    [File Part Parameter]
        Name of file attachment field as required by your destination api    
    [Multipart Params]
        A JSON object that includes the required multi part parameters to send files to your destination with the provided api

## Results
[Handler Error Message]
  Error message if an error was encountered and Error Handling is set to "Error Message".

[Files]
  Information on the files that were copied.

[Response Body]
  The body of the response from the upload API call.

## Sample Configuration


Error Handling:          Raise Error

Space Slug:

From Submission Id:      69825435-2b7b-11e7-983f-0748e4ca60e1

From Form Field Name:    File to Review

Attachment Index:  Index for the attachment to be copied

File Part Parameter: File attachement field name

Multipart Params:        '{
"fileName": "provide-test.json",
"folderPath": "/some-folder",
"options": "{\\"access\\":\\"PRIVATE\\"}"
}'

#### Note: 
This sample configuration is tailored to match uploading a file using the hubspot api. Find relevant documentation for refrence [here](https://developers.hubspot.com/docs/api/files/files).

Mulitpart Params hold any values needed by the api to process an upload exculding the actual file.


## Detailed Description
This handler uses the Kinetic Request CE REST API to retrieve the file the user submitted in one
submission to download it and upload the file to an external outlet using a REST API with url token/authentication.