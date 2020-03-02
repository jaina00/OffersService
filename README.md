This is a simple offers management REST service. This service uses Redis as data store to store Offers


There are 3 endpoints in this service

- POST /offers to create a new offer. Below is the sample payload
```json
{
"description":"item 1",
"price" : 10.0,
"currency": "sdfsdf",
"ttlInSeconds": 1
}
```
this endpoint returns 200 if all good. `ttlInSeconds` is used to set TTL when storing / SET in redis

While posting the data, users can send some invalid data. So some basic validations (min value for price, ttl and currency is within the list) are implemented using Cats Validation type.
  

- GET /offers/{offer-id} to retrieve offer.

- PATCH /offers/{offer-id}/CANCELLED to cancel an offer

- execute ./scripts/unit-test.sh to run the unit tests
- execute ./scripts/integration-test.sh  to run the integration tests

You may have to set permission to run the scripts

- sudo chmod 755 ./scripts/unit-test.sh
- sudo chmod 755 ./scripts/integration-test.sh
