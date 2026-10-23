package pt.ua.tqs.tuttibox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.Size;

@Entity
@Table(name = "compartments")
public class Compartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY on purpose: the allocation query locks only the compartment row, never the station row.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompartmentState state = CompartmentState.FREE;

    /** Set when a hardware problem was detected; cleared when the operator reactivates the compartment. */
    private boolean needsAttention;

    protected Compartment() {
    }

    public Compartment(Station station, String code, Size size) {
        this.station = station;
        this.code = code;
        this.size = size;
    }

    public Long getId() {
        return id;
    }

    public Station getStation() {
        return station;
    }

    public String getCode() {
        return code;
    }

    public Size getSize() {
        return size;
    }

    public CompartmentState getState() {
        return state;
    }

    public void setState(CompartmentState state) {
        this.state = state;
    }

    public boolean isNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention(boolean needsAttention) {
        this.needsAttention = needsAttention;
    }
}
