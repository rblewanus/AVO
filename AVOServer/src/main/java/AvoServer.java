import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Date;
import java.text.SimpleDateFormat;

public class AvoServer {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("AVO Server: Automatisch VetragingsOrakel Server gestart");
        System.out.println("de server luistert op topic AVOSignaleringIn");
        System.out.println("de server publiceert op queues AVOServerUit en AVOClientUit");
        System.out.println("==========================================================================");

        Thread handlerThread = new Thread(new VertragingsHandler());
        handlerThread.start();
    }


    public static class VertragingsHandler implements Runnable, ExceptionListener {

        private String treinSignalering;
        private String drglpuntSignalering;
        private String vertragingsBericht;

        public void run() {
            try {
                // Maak gebruik van ActiveMQ connection factory op localhost
                ActiveMQConnectionFactory connectionFactory =
                        new ActiveMQConnectionFactory("tcp://localhost:61616");

                // Maak de verbinding aan
                Connection connection = connectionFactory.createConnection();
                connection.start();
                connection.setExceptionListener(this);

                // Zet de ontvangende sessie op naar het juiste topic
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic("AVOSignaleringIn" );

                // Maak de consumer aan en handel de ingekomen vertragingen af
                MessageConsumer consumer = session.createConsumer(destination);

                while(true) {
                    Message message = consumer.receive(10000);
                    if(message != null) {
                        if(message instanceof TextMessage) {
                            TextMessage  textMessage = (TextMessage) message;
                            String textMessageContents = textMessage.getText();
                            System.out.println("-Ontvangen signalering: " + textMessageContents);
                            parseSignalering(textMessageContents);
                            bepaalVertraging();
                            verstuurVertragingServer();
                        }
                        else {
                            break;
                        }
                    }
                }

                consumer.close();
                session.close();
                connection.close();
            } catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        private void parseSignalering(String vertragingsbericht) {
            try {
                SAXBuilder builder = new SAXBuilder();
                InputStream instream = new ByteArrayInputStream(vertragingsbericht.getBytes("UTF-8"));
                Document doc = (Document) builder.build(instream);

                Element rootNode = doc.getRootElement();
                treinSignalering = rootNode.getChildText("Trein");
                drglpuntSignalering = rootNode.getChildText("Drglpunt");
            }
            catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        private void bepaalVertraging() {
            if(treinSignalering != null && drglpuntSignalering != null) {
                Element vertraging = new Element("Vertraging");
                Document doc = new Document(vertraging);
                vertraging.addContent(new Element("Trein").setText(treinSignalering));
                vertraging.addContent(new Element("Drglpunt").setText(drglpuntSignalering));

                // Bepaal nu automagisch de vertraging
                int vertragingsMinuten = 1;
                String huidigeTijd = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                if(drglpuntSignalering.equals("Ehv")) {
                    vertragingsMinuten = 120;
                } else {
                    vertragingsMinuten = ThreadLocalRandom.current().nextInt(1, 31);
                }
                vertraging.addContent(new Element("Vertraagd").setText("+" + String.valueOf(vertragingsMinuten)));
                vertraging.addContent(new Element("Tijd").setText(huidigeTijd));

                // Maak XML bericht
                XMLOutputter xmlOutput = new XMLOutputter();
                xmlOutput.setFormat(Format.getPrettyFormat());
                vertragingsBericht = xmlOutput.outputString(doc);

            } else
            {
                // kan geen vertraging bepalen zonder alle relevante gegevens
                System.out.println("-ERROR: kan geen vertraging bepalen!");
                vertragingsBericht = "";
            }
        }

        private void verstuurVertragingServer() {
            try {
                // Gebruik maken van ActiveMQ op localhost
                ActiveMQConnectionFactory connectionFactory =
                        new ActiveMQConnectionFactory("tcp://localhost:61616");
                Connection connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue("AVOServerUit");
                Destination clientdest = session.createQueue("AVOClientUit");
                MessageProducer producer = session.createProducer(destination);
                MessageProducer clientproducer = session.createProducer(clientdest);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                clientproducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                // Verstuur het bericht
                if(!vertragingsBericht.isEmpty()) {
                    TextMessage message = session.createTextMessage(vertragingsBericht);
                    System.out.println("-Verstuurde vertraging (Server): " + vertragingsBericht);
                    producer.send(message);
                    clientproducer.send(message);
                }

                // Opruimen verbinding
                session.close();
                connection.close();
            }
            catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }
    }
}
