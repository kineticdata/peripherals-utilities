# kinetic-filehub-adapter-cloud
Generic filestore implementation for interacting with common Cloud file stores (such as Amazon s3, 
Microsoft Azure, and Openstack Swift).  This adapter utilizes the jCloud http://jclouds.apache.org/ 
library to abstract the interactions with supported cloud solutions.

## Building
To manually build this adapter locally, run the following:  
`mvn clean install`

## Deploying to S3
To deploy this image to S3 manually, run the following.  You will need the Access Key and Secret for the IAM User that as permissions to the S3 buckets listed in the pom.xml file.

If `-SNAPSHOT` is included in the version reference, it will be published to the `snapshots/` directory, else to the `releases/` directory:  
`mvn -Diam-user-access-key-id=AN_ACCESS_KEY -Diam-user-secret-key=A_SECRET_KEY deploy`