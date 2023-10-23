
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService paymentService;
    @Mock
    private SeatReservationService reservationService;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    void setup() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void isAccountIdValid() {
        // Given
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        // When
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, adultRequest));

        // Then
        Assertions.assertEquals("Account ID is not valid.", exception.getMessage());
    }

    @Test
    void testAtleastOneAdultTicket() {
        // Given
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        // When
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest));

        // Then
        Assertions.assertEquals("Child and Infant tickets cannot be purchased without purchasing an Adult ticket.",
                exception.getMessage());
    }

    @Test
    void testSufficientAdultsForInfants() {
        // Given
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        // When
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));

        // Then
        Assertions.assertEquals("Infants should sit on an Adult's lap.", exception.getMessage());
    }

    @Test
    void testMinimumTicketsLimit() {
        // Given
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);

        // When
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adultRequest));

        // Then
        Assertions.assertEquals("Must purchase a minimum of 1 ticket.", exception.getMessage());
    }

    @Test
    void testMaximumTicketsLimit() {
        // Given
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 19);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        // When
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adultRequest, childRequest));

        // Then
        Assertions.assertEquals("Only a maximum of 20 tickets can be purchased at a time.", exception.getMessage());
    }

    @Test
    void testTicketPurchaseForAnAdultOnly() {
        // Given
        TicketTypeRequest typeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);

        // When
        ticketService.purchaseTickets(1L, typeRequest);

        // Then
        verify(paymentService, times(1)).makePayment(1L, 80);
    }

    @Test
    void testSeatReservationForAdultOnly() {
        // Given
        TicketTypeRequest typeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        Long accountId = 1L;
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        // When
        ticketService.purchaseTickets(accountId, adultRequest);

        // Then
        verify(paymentService, times(1)).makePayment(1L, 40); // (2 Adult tickets * £20 -> £40)
        verify(reservationService, times(1)).reserveSeat(1L, 2); // (2 Adult tickets)
        Assertions.assertEquals(2, typeRequest.getNoOfTickets());
    }

    @Test
    void testTicketPurchaseForAdultAndChildWithInfant() {
        // Given
        TicketTypeRequest typeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        // When
        ticketService.purchaseTickets(1L, typeRequest, childRequest, infantRequest);

        // Then
        verify(paymentService, times(1)).makePayment(1L, 110);
    }

    @Test
    void testSeatReservationForAdultAndChildWithInfant() {
        // Given
        Long accountId = 1L;
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        // When
        ticketService.purchaseTickets(accountId, adultRequest, childRequest, infantRequest);

        // Then
        verify(paymentService, times(1)).makePayment(1L, 70); // (2 Adult tickets * £20 + 3 Child tickets * £30 = (40+30)=70 )
        verify(reservationService, times(1)).reserveSeat(1L, 5); // (2 Adult tickets + 3 Child tickets + 1 Infant ticket)
    }
}
