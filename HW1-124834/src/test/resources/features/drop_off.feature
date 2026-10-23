Feature: Drop off a parcel at the locker terminal (UC3)
  As a courier, I want to enter the tracking code so that the reserved compartment opens
  and the parcel becomes available for the recipient.

  Background:
    Given a station "Aveiro Station" with compartments "M,M"
    And a parcel of size "M" is registered at "Aveiro Station"

  Scenario: The courier deposits the parcel
    When the courier drops off the parcel at "Aveiro Station"
    Then the terminal answers "OK"
    And the parcel state is "READY_FOR_PICKUP"
    And the recipient receives a pickup code

  Scenario: The parcel is not deposited within the drop-off deadline
    When 5 days and 1 minute pass
    And the deadline check runs
    Then the parcel state is "CANCELLED"
    And the parcel's compartment is "FREE"

  Scenario: The door of the reserved compartment does not open
    Given the door of the parcel's compartment fails to open
    When the courier drops off the parcel at "Aveiro Station"
    Then the terminal answers "OK"
    And the original compartment is "OUT_OF_SERVICE"
    And the parcel is in a different compartment
    And the operator is notified
