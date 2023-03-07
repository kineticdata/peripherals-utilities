{
  'info' => {
    'api_username' => "",
    'api_password' => "",
    'api_location' => "",
    'enable_debug_logging' => 'true'
  },
  'parameters' => {
    'error_handling' => 'Raise Error',
    'kapp_slug' => 'services',
    'form_slug' => 'pdf-signature-test',
    'filename' => 'SignedDocument',
    'submission_id'=> '4c31e018-fda9-11ec-8db2-e58d8bda3aca',
    'attachment_field_name' => 'Signature',
    'xhtml' => '
    <html>
<head>
<style type="text/css">

  style { display: none }
  p { display: block; color: green }
  p.note { color: red }
  b {font-weight: bold }
  hr {margin-top: 1rem; margin-bottom: 1rem; border-top:1px solid black}

</style>
<title>Your Title Here</title>
</head>
<body>
<hr></hr>
<a href="http://somegreatsite.com=">Link Name</a>
is a link to another nifty site
<h1>This is a Header</h1>
<h2>This is a Medium Header</h2>
Send me mail at <a href="mailto:support@yourcompany.com">
support@yourcompany.com</a>.
<p> This is a new paragraph!</p>
<p> <b>This is a new paragraph!</b></p>
<p> <b>WAHOO!</b> </p>
<br></br>
<br></br>
<br></br>
<b><i>This is a new sentence without a paragraph break, in bold italics.</i></b>
<hr></hr>
<img src="##Image##" width="200"/>
</body>
</html>'
  }
}
