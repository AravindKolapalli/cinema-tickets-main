package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

public class TicketServiceImpl implements TicketService {

    private static final int MAXIMUM_TICKETS = 20;
    private static final int MINIMUM_TICKETS = 1;
    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.paymentService = ticketPaymentService;
        this.reservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateTicketPurchase(accountId, ticketTypeRequests);

        // Calculating totalAmountToPay and the totalSeats.
        int totalAmountToPay = 0;
        int totalSeats = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            int noOfTickets = ticketTypeRequest.getNoOfTickets();
            TicketTypeRequest.Type ticketType = ticketTypeRequest.getTicketType();

            if (ticketType == TicketTypeRequest.Type.ADULT) {
                totalAmountToPay += noOfTickets*20;
                totalSeats += noOfTickets;
            } else if (ticketType == TicketTypeRequest.Type.CHILD){
                totalAmountToPay += noOfTickets*10;
                totalSeats += noOfTickets;
            }
        }
        // Make the payment and reserve the seats.
        paymentService.makePayment(accountId, totalAmountToPay);
        reservationService.reserveSeat(accountId, totalSeats);
    }

    // Validating tickets based on requirements
    private void validateTicketPurchase(Long accountId, TicketTypeRequest... typeRequests) throws InvalidPurchaseException {

        if (accountId <1) {
            throw new InvalidPurchaseException("Account ID is not valid.");
        }

        // Checking if there is at least one adult ticket as child and infants should be accompanied
        if (SumOfSameTypeOfTickets(TicketTypeRequest.Type.ADULT, typeRequests) < MINIMUM_TICKETS &&
                (SumOfSameTypeOfTickets(TicketTypeRequest.Type.CHILD, typeRequests) > 0 ||
                        SumOfSameTypeOfTickets(TicketTypeRequest.Type.INFANT, typeRequests) > 0)) {
            throw new InvalidPurchaseException("Child and Infant tickets cannot be purchased without purchasing an Adult ticket.");
        }

        // Checking if the number of infants is less than or equal to the number of adults.
        if (SumOfSameTypeOfTickets(TicketTypeRequest.Type.ADULT, typeRequests) <
                SumOfSameTypeOfTickets(TicketTypeRequest.Type.INFANT, typeRequests)) {
            throw new InvalidPurchaseException("Infants should sit on an Adult's lap.");
        }

        // Checking if the total number of tickets is within the limits.
        int totalNoTickets = SumOfSameTypeOfTickets(TicketTypeRequest.Type.ADULT, typeRequests) +
                SumOfSameTypeOfTickets(TicketTypeRequest.Type.CHILD, typeRequests) +
                SumOfSameTypeOfTickets(TicketTypeRequest.Type.INFANT, typeRequests);

        if (totalNoTickets < MINIMUM_TICKETS) {
            throw new InvalidPurchaseException("Must purchase a minimum of 1 ticket.");
        }
        if (totalNoTickets > MAXIMUM_TICKETS) {
            throw new InvalidPurchaseException("Only a maximum of 20 tickets can be purchased at a time.");
        }
    }


    private int SumOfSameTypeOfTickets(TicketTypeRequest.Type type, TicketTypeRequest... ticketTypeRequests) {
        return Stream.of(ticketTypeRequests)
                .filter(ticketTypeRequest -> ticketTypeRequest.getTicketType().equals(type))
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }
}
