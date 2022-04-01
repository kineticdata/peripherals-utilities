{
  'info' => {
    'api_server' => 'https://someurl',
    'api_username' => 'someuser',
    'api_password' => 'testing',
    'space_slug' => '',
    'destination_key' => 'hapikey=xxxxx.xxxxxxxx',
    'destination_api' => 'https://api.hubapi.com/files/v3/files',
    'enable_debug_logging' => 'Yes'
  },
  'parameters' => {
    'error_handling' => 'Raise Error',
    'space_slug' => '',
    'field_name' => 'Uploads',
    'submission_id' => '',
    'multipart_params' =>'{
      "fileName": "provide-test.json",
      "folderPath": "/kd-cp-attachments",
      "options": "{\\"access\\":\\"PRIVATE\\"}"
    }'
  }
}