package nz.jpp.TicketService;

import java.util.Set;

public interface SeatAllocator {

	/**
	 * The number of seats in the venue that are neither held nor reserved
	 *
	 * @return the number of tickets available in the venue
	 */
	public int numSeatsAvailable();
	
	public Set<Integer> getSeats(int numSeats);
	
	public void returnSeat(Integer seat);
}
