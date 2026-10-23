package pt.ua.tqs.tuttibox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.domain.TerminalLockout;

@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private double latitude;

    private double longitude;

    @Embedded
    private TerminalLockout lockout = new TerminalLockout();

    protected Station() {
    }

    public Station(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Coordinates coordinates() {
        return new Coordinates(latitude, longitude);
    }

    public TerminalLockout getLockout() {
        if (lockout == null) {
            lockout = new TerminalLockout();
        }
        return lockout;
    }
}
