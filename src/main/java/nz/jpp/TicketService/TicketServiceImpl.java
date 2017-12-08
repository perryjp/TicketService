package nz.jpp.TicketService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketServiceImpl implements TicketService {
	
	//we keep track of reserved seats and held seat, seatAllocator is responsible for tracking available seats
	private final ConcurrentHashMap<Integer,Set<Integer>> reservedSeats = new ConcurrentHashMap<Integer,Set<Integer>>();
	private final ConcurrentHashMap<SeatHoldKey,SeatHold> heldSeats = new ConcurrentHashMap<SeatHoldKey,SeatHold>();
	private final SeatAllocator seatAllocator;
	private final int seatHoldTTLinSeconds;
	
	public TicketServiceImpl(SeatAllocator seatAllocator, int seatHoldTTLinSeconds) {
		this.seatAllocator = seatAllocator;
		this.seatHoldTTLinSeconds = seatHoldTTLinSeconds;
				
		runSeatHoldSweeper();
	}

	public void runSeatHoldSweeper() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		Runnable task = () -> {
//			System.err.println("Purging expired seat holds");
			LocalDateTime now = LocalDateTime.now();
			ArrayList<Integer> expiredSeats = new ArrayList<Integer>();
			try {
				heldSeats.values().forEach(seatHold -> {
					if(seatHold.isExpired(now) && heldSeats.remove(seatHold.getKey(), seatHold)) {
//						System.err.format("Expiring %d seats with id %d for customer %s\n", seatHold.getSeats().size(), seatHold.getSeatHoldId(), seatHold.getEmail());
						expiredSeats.addAll(seatHold.getSeats());
					}
					
				});
			} finally {
				//place in a finally block to make sure we don't lose any seats that have been removed from the hold list
				if (!expiredSeats.isEmpty()) 
					expiredSeats.forEach(seat -> seatAllocator.returnSeat(seat));
			}
		};

		int initialDelay = seatHoldTTLinSeconds/2;
		int period = seatHoldTTLinSeconds/2;
		executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);

	}
	
	/**
	 * The number of seats in the venue that are neither held nor reserved
	 *
	 * @return the number of tickets available in the venue
	 */
	public int numSeatsAvailable() {
		return seatAllocator.numSeatsAvailable();
	}

	/**
	 * Find and hold the best available seats for a customer
	 *
	 * @param numSeats
	 *            the number of seats to find and hold
	 * @param customerEmail
	 *            unique identifier for the customer
	 * @return a SeatHold object identifying the specific seats and related
	 *         information or null if numSeats are not avilable together in the same row
	 */
	public SeatHold findAndHoldSeats(int numSeats, String customerEmail) {
		Set<Integer> seats = seatAllocator.getSeats(numSeats);
		if (seats == null || seats.isEmpty())
			return null;
					
		SeatHold seatHold = new SeatHold(customerEmail, seatHoldTTLinSeconds, seats);
		heldSeats.put(seatHold.getKey(), seatHold);
		return seatHold;
	}

	
	/**
	 * Commit seats held for a specific customer
	 *
	 * @param seatHoldId
	 *            the seat hold identifier
	 * @param customerEmail
	 *            the email address of the customer to which the seat hold is
	 *            assigned
	 * @return a reservation confirmation code or SEAT_HOLD_ID_EXPIRED/SEAT_HOLD_ID_NOT_FOUND on error
	 */
	public String reserveSeats(int seatHoldId, String customerEmail) {
		SeatHoldKey key = new SeatHoldKey(seatHoldId, customerEmail);
		try {
			SeatHold seatHold = heldSeats.get(key); //may throw a NullPointerException!
			
			//if the seatHold is expired or we failed to remove it (another thread removed it) throw an exception
			if(seatHold.isExpired(LocalDateTime.now()) || !heldSeats.remove(key, seatHold))
				return SEAT_HOLD_ID_EXPIRED;

			reservedSeats.put(key.hashCode(), seatHold.getSeats());
			
			return Integer.toHexString(key.hashCode());
		} catch(NullPointerException e) {
			return SEAT_HOLD_ID_NOT_FOUND;
		}
	}
	
}
