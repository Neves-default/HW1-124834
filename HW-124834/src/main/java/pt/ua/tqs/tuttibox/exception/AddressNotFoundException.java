package pt.ua.tqs.tuttibox.exception;

public class AddressNotFoundException extends NotFoundException {
    public AddressNotFoundException(String address) {
        super("ADDRESS_NOT_FOUND", "Address not found: " + address);
    }
}
