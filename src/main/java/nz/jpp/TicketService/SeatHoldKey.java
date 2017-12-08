package nz.jpp.TicketService;

import java.util.Objects;

public class SeatHoldKey {
	private final int seatHoldId;
	private final String email;
	
	public SeatHoldKey(int seatHoldId, String email) {
		this.seatHoldId = seatHoldId;
		this.email = email;
	}
	
	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof SeatHoldKey)) {
            return false;
        }
        SeatHoldKey other = (SeatHoldKey) o;
        return this.seatHoldId == other.seatHoldId && this.email == other.email;
	}
	
	@Override
	public int hashCode() {
        return Objects.hash(seatHoldId, email);
		
	}
}
