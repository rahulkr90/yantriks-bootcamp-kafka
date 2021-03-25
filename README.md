# yantriks-bootcamp-kafka
************************* To use this application follow below steps ****************************
Run the executable jar:
java -jar yantriks-bootcamp-kafka-0.0.1-app.jar

Hit the URL:
http://localhost:8094/kafka-rest-services to post the message into custom topic

List of Topics will be used (yantriksbootcamp is added in the code for the topic no need to pass this in the message):
input-topic = custom-inventory-update-feed-integration-topic-US
target-topic = inventory-update-feed

This consumer will listen to the input-topic and transform the message as requested in the use case..
After massaging the input it will form the message into the required format and then post again to the 
kafka-res-service from there it will be picked up the inventory-kafka-streamer and supply will get updated