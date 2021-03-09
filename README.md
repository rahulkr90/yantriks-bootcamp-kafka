# yantriks-bootcamp-kafka
Codebase for kafka assignment

************************* To use this application follow below steps ****************************
Run the executable jar:
java -jar yantriks-bootcamp-kafka-0.0.1-app.jar

Hit the URL:
http://localhost:8081/send-employee-detail with sample payload from samplePayload.json

List of Topics will be used (yantriksbootcamp is added in the code for the topic no need to pass this in the message):
input-topic = yantriksbootcamp-employee-feed-topic
target-topic = yantriksbootcamp-employee-feed-target-topic
DLQ-Topic = yantriksbootcamp-employee-feed-topic-DLQ 
