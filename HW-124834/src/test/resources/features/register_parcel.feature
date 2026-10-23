Feature: Register a parcel for a station (UC2)
  As a partner, I want to register a parcel for a station so that a compartment is reserved
  and I get a tracking code.

  Background:
    Given a station "Aveiro Station" with compartments "S,M"

  Scenario: The parcel gets a compartment of the same size
    When the partner registers a parcel of size "M" for "Aveiro Station"
    Then the registration succeeds with state "CREATED"
    And the parcel is assigned a compartment of size "M"

  Scenario: A larger compartment is used when the exact size is full
    Given a parcel of size "S" is already registered at "Aveiro Station"
    When the partner registers a parcel of size "S" for "Aveiro Station"
    Then the registration succeeds with state "CREATED"
    And the parcel is assigned a compartment of size "M"

  Scenario: The station is full
    Given a parcel of size "S" is already registered at "Aveiro Station"
    And a parcel of size "M" is already registered at "Aveiro Station"
    When the partner registers a parcel of size "S" for "Aveiro Station"
    Then the registration is rejected with "NO_FREE_COMPARTMENT"
