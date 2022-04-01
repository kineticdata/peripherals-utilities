# Kinetic Request CE Attachment Upload With API
    This handler is used to copy an attachment from one submission to another.

## Parameters
    [Error Handling]
        How to handle error conditions in the handler: raise the error, or return error message
    [Space Slug]
        Slug of the Space where the handler should be imported
    [From Submission Id]
        Submission Id that contains the attached file to copy.
    [From Form Field Name]
        Name of the file attachment field on the Kinetic Request CE form to copy from
    [Multipart Params]
        A JSON object that includes the required multi part parameters to send files to your destination with the provided api 
 ## Results
[Handler Error Message]
  Error message if an error was encountered and Error Handling is set to "Error Message".

[Files]
  Information on the files that were copied.

[Space Slug]
  The space slug that was used.

## Sample Configuration


Error Handling:          Raise Error

Space Slug:

From Submission Id:      69825435-2b7b-11e7-983f-0748e4ca60e1

From Form Field Name:    File to Review

Multipart Params:        '{
"fileName": "provide-test.json",
"folderPath": "/kd-cp-attachments",
"options": "{\\"access\\":\\"PRIVATE\\"}"
}'



## Detailed Description
This handler uses the Kinetic Request CE REST API to retrieve the file the user submitted in one
submission to download it and upload the file to an external outlet using a REST API with url token/authentication.