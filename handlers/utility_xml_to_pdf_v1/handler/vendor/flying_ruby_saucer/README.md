FlyingRubySaucer
============

A ruby wrapper for the Flying Saucer JAVA XHTML renderer (see http://code.google.com/p/flying-saucer/).
This gem does not require jruby, but you need a working JAVA runtime environment. It then takes
a HTML file, converts it to PDF via Flying Saucer and returns the PDF file back to the caller.

That's the whole magic.

It also comes with a Rails 3.x-style renderer, so you can invoke it directly from your Rails controllers.

This gem is extracted from Cornerstore (www.cornerstore.io). We used it in production nearly three
years, without any trouble.


Included Version of Flying Saucer
=================================
This gem includes version R8 of Flying Saucer, distributed under the LGPL.


Example
=======
Using FlyingRubySaucer directly

```ruby
require 'flying_ruby_saucer'
pdf_source = FlyingRubySaucer::Generator.string_to_pdf(html_string)
```

If something goes wrong, a StandardError containing the console output will be thrown.

Using Rails

With rails you simply use all the respond_to format beauty. Just tell your controller to
respond_to :pdf for the given action.

```ruby
def show
  # Get an article to display
  @article = Article.find(params[:id])
  
  # Respond to the client. If the client requested the PDF mime-type
  # FlyingRubySaucer will render the html template, convert that into PDF
  # and respond with the PDF data.
  respond_with @article
end
```
