package nz.jpp.TicketService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;



public class SeatHold {
	//TODO: is this really a great way of generating the ID? not exactly "secure"
	private static final AtomicInteger idGenerator = new AtomicInteger(0);
	
	private final int seatHoldId;
	private final String email;
	private final LocalDateTime expirationTime;
	private final Set<Integer> seats;
	
	public SeatHold(String email, int ttlSeconds, Set<Integer> seats) {
		this.seatHoldId = idGenerator.getAndIncrement();
		this.seats = new HashSet<Integer>(seats);
		this.email = email;
		this.expirationTime = LocalDateTime.now().plusSeconds(ttlSeconds);
	}
	
	public int getSeatHoldId() { return seatHoldId; }
	public String getEmail() { return email; }
	
	public SeatHoldKey getKey() { return new SeatHoldKey(seatHoldId, email); }
	
	public Set<Integer> getSeats() {
		return new HashSet<Integer>(seats);
	}
	
	public boolean isExpired(LocalDateTime now) {
		return expirationTime.isBefore(now);
	}
	
	public boolean isLive(LocalDateTime now) {
		return !isExpired(now);
	}
}