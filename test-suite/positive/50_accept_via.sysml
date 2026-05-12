package AcceptViaTest {
    
    // Example from 7.17.7
    part controller {
        item def SensorReading;
        
        action accepting {
            action acceptReading {
                accept reading : SensorReading via controller;
            }
            
            action acceptReading2 {
                // The following accept action is equivalent to the one above,
                // but it does not name the accept action or the payload parameter.
                accept SensorReading via controller;
            }
        }
    }

    // Example from 7.14.2
    part def DistributedSystem {
        item def Request;
        item def Response; 
        
        part client {
            port clientPort;
            action clientBehavior {
                send new Request() via clientPort;
                then accept Response via clientPort;
            }
        }
        
        part server {
            port serverPort;
            action serverBehavior {
                accept Request via serverPort;
                then send new Response() via serverPort;
            }
        }
        
        // Transfers from the clientPort automatically target the serverPort and vice versa.
        interface client.clientPort to server.serverPort;
    }
}
