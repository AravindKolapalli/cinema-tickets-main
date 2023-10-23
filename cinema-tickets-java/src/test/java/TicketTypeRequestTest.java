import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static org.junit.Assert.assertEquals;

public class TicketTypeRequestTest {

    @Test
    void testNumberOfTickets() {
        var ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        assertEquals(2, ticketTypeRequest.getNoOfTickets());
    }

    @Test
    void testAdultTicketType() {
        var ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        assertEquals(TicketTypeRequest.Type.ADULT, ticketTypeRequest.getTicketType());
    }

    @Test
    void testChildTicketType() {
        var ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        assertEquals(TicketTypeRequest.Type.CHILD, ticketTypeRequest.getTicketType());
    }

    @Test
    void testInfantTicketType() {
        var ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        assertEquals(TicketTypeRequest.Type.INFANT, ticketTypeRequest.getTicketType());
    }

}
