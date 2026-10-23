Feature: Supervise a station (UC6)
  As an operator, I want to handle expired parcels and faulty compartments
  so that the station keeps working.

  Background:
    Given a station "Aveiro Station" with compartments "M,L"
    And a parcel of size "M" is ready for pickup at "Aveiro Station"

  Scenario: Removing an expired parcel returns it and frees the compartment
    When 5 days and 1 minute pass
    And the deadline check runs
    And the operator registers the removal of the parcel
    Then the parcel state is "RETURNED"
    And the parcel's compartment is "FREE"

  Scenario: A parcel that is not expired cannot be removed
    When the operator registers the removal of the parcel
    Then the operation is rejected with "INVALID_TRANSITION"

  Scenario: An occupied compartment cannot be put out of service
    When the operator puts the parcel's compartment out of service
    Then the operation is rejected with "COMPARTMENT_NOT_EMPTY"
