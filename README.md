# AR2T
AmazonReviews2Tweets is designed to extract tweets related to amazon reviews. It is designed to parse [amazon review 
datasets](https://nijianmo.github.io/amazon/index.html) and fetch tweets related to the products. Our overall motivation
for this is to integrate 3rd party data into recommender systems.

See `src/main/resources/application.conf` for configuration settings. In the future I will be releasing a dockerized
version of this in the future.

## Technologies Used
- Scala, along with some Java references for libraries that I couldn't find good Scala counterparts for
- Apache Spark
- (upcoming) Postgres+JDBC
