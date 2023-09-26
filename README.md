> :warning: **This project is no longer maintained and unlikely to work**. Due to recent Twitter API changes that impacted the Twitter4S project, this project is non-functional.

# AR2T

AmazonReviews2Tweets is designed to extract tweets related to amazon reviews. It is designed to parse [amazon review 
datasets](https://nijianmo.github.io/amazon/index.html) and fetch tweets related to the products. Our overall motivation
for this is to integrate 3rd party data into recommender systems.

See `src/main/resources/application.conf` for configuration settings. In the future I will be releasing a dockerized
version of this in the future.

## Technologies Used
- Scala, along with some Java references for libraries that I couldn't find good Scala counterparts for
- [Apache Spark](https://spark.apache.org/)
- [Twitter4s](https://github.com/DanielaSfregola/twitter4s), an excellent twitter API wrapper written in Scala.
- (upcoming) Postgres+JDBC

## Twitter API Integration

As per Twitter4s's documentation, expose the following environment variables at runtime:
```bash
export TWITTER_CONSUMER_TOKEN_KEY='my-consumer-key'
export TWITTER_CONSUMER_TOKEN_SECRET='my-consumer-secret'
export TWITTER_ACCESS_TOKEN_KEY='my-access-key'
export TWITTER_ACCESS_TOKEN_SECRET='my-access-secret'
```
