# Kinetic Bridgehub Adapter Generic Rest
This adapter can be used to make generic rest calls that require basic auth.  The adapter supports either JSON or XML responses, and always returns the data parsed into JSON. JSON Path can be used to retrieve specific parts of the response.
___
## Adapter Configurations
Name | Description
------------ | -------------
Username | Username of user to the source system
Password | Privileged user's password
URL Origin | Web address to an external server (ex: https://acme.service-now.com)
___
## Supported Structures
The path to the asset that does not contain variables.  _Although the Kinetic Data Platform Adapter is the preferred bridge integration the examples include configuration to get Forms assets from the Kinetic Data Platform._

### Example Structures:
* To get **Knowledge Articles** from serviceNow: /api/sn_km_api/knowledge/articles
* To get **Forms** from Kinetic Data: /app/api/v1/kapps/services/forms

___
## Example Qualification Mapping
There are multiple patterns used in the qualification mapping.  The pattern that is required depends on whether there is a variable in the path to the asset, query parameters are to be passed to the source system and if a **JSON Path** root is used to access nested response data.

The qualification can have any combination of **path with variables**, query parameters or a **JSON root** accessor.  Below are examples.  _Although the Kinetic Data Platform Adapter is the preferred bridge integration the examples include configuration to get Forms assets from the Kinetic Data Platform._  The below examples assume that the Bridge Model has the above Structures configured.

### Query Parameter Only Examples:
* To query for **Knowledge Articles** from serviceNow: q=title="how to"
* To query for **Forms** from Kinetic Data: q=name *=* "HR"&limit=5

### Path with Variable Examples:
* To get a **Knowledge Article** from serviceNow: /${parameters("Article Id")}
* To get a **Form** from Kinetic Data: /${parameters("Form Slug")}

### JSON Root accessor:
* To access **Knowledge Articles** from serviceNow response: _:$.result.articles_
``` javascript
 {
   result: {
     articles: [{},...,{}]
   }
 }
```

* To access **Form** from Kinetic Data response: _:$['forms']_
``` javascript
 {
   forms: [{},...,{}]
 }
```

* To access **items** from the below sample XML response: _:$.rss.channel.item_
``` xml
<rss>
  <channel>
    <item>
      <id>1</id>
      <title>Sample One</title>
    </item>
    <item>
      <id>2</id>
      <title>Sample Two</title>
    </item>
  </channel>
</rss>
```

### Example of all three components:
* /${parameters("Form Slug")}/submissions?q=values['Status']=${parameters("Status")}&include=values:$.submissions..values
``` javascript
 {
   submissions: [{
     ...,
     values: {...}
   },...,{
     ...,
     values: {...}
   }]
 }
```
___
## Notes
* [JsonPath](https://github.com/json-path/JsonPath#path-examples) can be used to access nested values. The root of the path is values.
* Attributes mappings are case sensitive.
* The Generic Adapter requires a JSON or XML response as a payload (type of response needs to be set when configuring the bridge in your space).
