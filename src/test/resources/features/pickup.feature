Feature: Pick up a parcel at the locker terminal (UC4)
  As a recipient, I want to enter my pickup code so that the compartment opens
  and I can collect my parcel.

  Background:
    Given a station "Aveiro Station" with compartments "M"
    And a parcel of size "M" is ready for pickup at "Aveiro Station"

  Scenario: The recipient collects the parcel
    When the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "OK"
    And the parcel state is "PICKED_UP"
    And the parcel's compartment is "FREE"

  Scenario: The pickup code is single use
    Given the recipient has already collected the parcel at "Aveiro Station"
    When the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "INVALID_CODE"

  Scenario: The terminal is blocked after 5 wrong codes
    When a wrong pickup code is entered 5 times at "Aveiro Station"
    And the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "TERMINAL_BLOCKED"
    When 2 minutes and 0 seconds pass
    And the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "OK"

  Scenario: The parcel is not collected within the pickup deadline
    When 5 days and 1 minute pass
    And the deadline check runs
    Then the parcel state is "EXPIRED"
    When the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "INVALID_CODE"

  Scenario: The door does not open at pickup
    Given the door of the parcel's compartment fails to open
    When the recipient enters the correct pickup code at "Aveiro Station"
    Then the terminal answers "HARDWARE_FAILURE"
    And the parcel state is "READY_FOR_PICKUP"
    And the operator is notified
