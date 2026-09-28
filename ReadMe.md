\# Duplicate Payment \& Idempotency



A production-oriented Spring Boot mini-project demonstrating how duplicate payments can occur under retries and concurrent requests, and how to progressively solve the problem using \*\*Idempotency Keys, Database Constraints, Transactions, and Concurrency Testing\*\*.



The project intentionally evolves from a simple implementation into a production-hardened design.



\---



\## 🎯 Problem Statement



Consider a payment API:



```http

POST /payments

{

&#x20; "userId": 101,

&#x20; "amount": 500

}



The server processes the payment successfully, but the response is lost because of a network timeout.



The client does not know whether the payment succeeded.



So the client retries the request.



Without idempotency protection:



Request 1

&#x20;  ↓

Payment processed

&#x20;  ↓

Database INSERT

&#x20;  ↓

Response lost

&#x20;  ↓

Client retries

&#x20;  ↓

Request 2

&#x20;  ↓

Payment processed again

&#x20;  ↓

Another database INSERT



Result:



One logical payment

&#x20;       ↓

Multiple payment records

&#x20;       ↓

Potential duplicate charge

🏗️ Tech Stack

Java 17

Spring Boot

Spring MVC

Spring Data JPA

Hibernate

MySQL

Maven

JMeter

Git / GitHub

🔄 Project Evolution



The project was intentionally developed through multiple versions.



V1 → Basic Payment Implementation

&#x20;       ↓

V2 → Reproduce Duplicate Payment

&#x20;       ↓

V3 → Naive Duplicate Check

&#x20;       ↓

V4 → Idempotency Key

&#x20;       ↓

V5 → Database Protection

&#x20;       ↓

V6 → Transaction Investigation

&#x20;       ↓

V7 → Production Hardening

V1 — Basic Payment Implementation



The initial implementation simply:



Receive Request

&#x20;     ↓

Create Payment

&#x20;     ↓

Process Payment

&#x20;     ↓

Save Payment

&#x20;     ↓

Return Response



There was:



No idempotency key

No duplicate protection

No concurrency protection



The implementation worked correctly for a single request.



However, it did not protect against repeated or concurrent requests.



V2 — Reproducing the Duplicate Payment



Using JMeter, multiple concurrent requests were sent with:



{

&#x20; "userId": 101,

&#x20; "amount": 1000

}



10 concurrent requests resulted in:



10 Requests

&#x20;    ↓

10 Payment Objects

&#x20;    ↓

10 INSERT statements

&#x20;    ↓

10 Payment Records



The database was behaving correctly.



The problem was the application design.



Important observation



The database cannot determine that:



userId = 101

amount = 1000



represents the same logical payment.



A user can legitimately make multiple payments for the same amount.



V3 — Naive Duplicate Check



A first attempt was:



existsByUserIdAndAmount(...)



The idea was:



Check whether payment exists

&#x20;       ↓

If not found

&#x20;       ↓

Create payment



But under concurrency:



Request A → CHECK → Not Found

Request B → CHECK → Not Found

Request C → CHECK → Not Found

...

&#x20;       ↓

All requests continue

&#x20;       ↓

All create payments



This demonstrated the classic:



CHECK → SAVE



race condition.



Key Lesson



Application-level checking alone cannot guarantee uniqueness under concurrent requests.



Also:



userId + amount



is not a valid identity for a logical payment operation.



V4 — Idempotency Key



The solution needs an identifier for the logical operation.



The client sends:



Idempotency-Key: PAYMENT-12345



Example:



POST /payments

Content-Type: application/json

Idempotency-Key: PAYMENT-12345

{

&#x20; "userId": 101,

&#x20; "amount": 500

}



The idempotency key represents:



"This is the same logical payment operation."



Now a retry can reuse the same key.



First Request

PAYMENT-12345

&#x20;     ↓

Payment created



Retry

PAYMENT-12345

&#x20;     ↓

Same logical operation



However, the application-level existence check was still vulnerable to concurrent requests.



V5 — Database Protection



The database became the final authority.



A unique constraint was added:



UNIQUE (idempotency\_key)



This creates a database invariant:



One idempotency key

&#x20;       ↓

At most one payment record



Under concurrency:



10 Requests

&#x20;    ↓

Same Idempotency-Key

&#x20;    ↓

Database

&#x20;    ↓

1 INSERT succeeds

9 INSERT attempts violate UNIQUE constraint



The application catches the resulting:



DataIntegrityViolationException



and retrieves the already-created payment.



Important Engineering Principle

Application check

&#x20;       ↓

Optimization / fast path



Database UNIQUE constraint

&#x20;       ↓

Correctness guarantee



The application should not rely only on the first check.



V6 — Transaction Boundary



The next question was:



What actually happens when concurrent requests reach the database?



Transaction boundaries were investigated using:



@Transactional



The important distinction was between:



Repository-level transaction



and:



Service-level transaction



The project demonstrated that transaction boundaries affect:



Commit

Rollback

Exception propagation

Hibernate flush behavior

Database visibility

Concurrent inserts



A duplicate-key violation can cause the transaction to roll back.



Therefore, exception handling must consider where the transaction begins and ends.



V7 — Production Hardening



The final version separates the transaction boundary from the outer payment orchestration.



PaymentService

&#x20;     ↓

PaymentTransactionService

&#x20;     ↓

@Transactional

&#x20;     ↓

paymentRepo.save()

&#x20;     ↓

MySQL



For a new idempotency key:



INSERT

&#x20; ↓

COMMIT

&#x20; ↓

Return Payment



For a duplicate key:



INSERT

&#x20; ↓

UNIQUE constraint violation

&#x20; ↓

Transaction rollback

&#x20; ↓

Exception returned to outer service

&#x20; ↓

Find existing payment

&#x20; ↓

Return existing result

🔐 Same Key + Different Request



An important edge case is:



Request 1



Idempotency-Key: PAYMENT-123



userId = 101

amount = 500



Later:



Request 2



Idempotency-Key: PAYMENT-123



userId = 101

amount = 900



The key has already been associated with a different operation.



The application therefore rejects the request rather than returning the original payment as if the requests were identical.



Comparison is performed using:



existing.getUserId()



and:



existing.getAmount().compareTo(request.getAmount())

🚫 Idempotency-Key Validation



The API requires:



Idempotency-Key



Missing or blank keys are rejected.



Example:



Missing Idempotency-Key

&#x20;       ↓

HTTP 400



This prevents requests from entering the payment workflow without an operation identity.



🧪 Concurrency Testing



JMeter was used to reproduce real concurrent traffic.



Example configuration:



Threads: 10

Ramp-Up: 0

Loop Count: 1



Headers:



Content-Type: application/json

Idempotency-Key: PAY-CONCURRENT-1



Request:



{

&#x20; "userId": 101,

&#x20; "amount": 1000

}



Expected final result:



10 concurrent requests

&#x20;       ↓

Same Idempotency-Key

&#x20;       ↓

1 Payment record

&#x20;       ↓

Same paymentReference returned

🧪 Final Test Cases

Test	Expected Result

New payment	Payment created

Same key + same request	Existing payment returned

Same key + different amount	Request rejected

Same key + different user	Request rejected

Different keys	Independent payments created

10 concurrent same-key requests	Only 1 payment stored

Missing Idempotency-Key	HTTP 400

Blank Idempotency-Key	HTTP 400

Negative amount	HTTP 400

🔄 Final Request Flow

&#x20;                   POST /payments

&#x20;                         |

&#x20;                         ↓

&#x20;               Validate Request

&#x20;                         |

&#x20;                         ↓

&#x20;              Validate Idempotency-Key

&#x20;                         |

&#x20;                         ↓

&#x20;               PaymentService

&#x20;                         |

&#x20;                         ↓

&#x20;            Create Payment Object

&#x20;                         |

&#x20;                         ↓

&#x20;            PaymentTransactionService

&#x20;                         |

&#x20;                         ↓

&#x20;                  @Transactional

&#x20;                         |

&#x20;                         ↓

&#x20;               paymentRepo.save()

&#x20;                         |

&#x20;                ┌────────┴────────┐

&#x20;                ↓                 ↓

&#x20;            New Key          Duplicate Key

&#x20;                ↓                 ↓

&#x20;             INSERT       UNIQUE violation

&#x20;                ↓                 ↓

&#x20;             COMMIT            ROLLBACK

&#x20;                ↓                 ↓

&#x20;            SUCCESS       Fetch existing

&#x20;                                  |

&#x20;                                  ↓

&#x20;                         Compare request data

&#x20;                                  |

&#x20;                        ┌─────────┴─────────┐

&#x20;                        ↓                   ↓

&#x20;                     Same data         Different data

&#x20;                        ↓                   ↓

&#x20;                 Return existing        Reject

🧠 Key Engineering Lessons

1\. Idempotency is about logical operations



An idempotency key identifies:



WHICH operation?



While:



userId → WHO?

amount → HOW MUCH?

2\. Check-before-save is not enough



This:



if (!exists()) {

&#x20;   save();

}



is not atomic.



Concurrent requests can all observe:



NOT FOUND



before any request inserts the record.



3\. Database constraints enforce invariants



The database should protect critical correctness rules.



UNIQUE(idempotency\_key)



provides a guarantee that application-level checking alone cannot provide.



4\. Transactions define boundaries



@Transactional determines the transaction boundary and influences:



Commit

Rollback

Exception propagation

Flush behavior

5\. Duplicate-key exceptions can be business-flow signals



A duplicate-key violation does not necessarily mean:



Payment failed



It can mean:



Another request already completed this logical operation.



The application can then retrieve the existing payment and return the same result.



📂 Project Structure

src

&#x20;└── main

&#x20;     └── java

&#x20;          └── com.series.payment\_service

&#x20;               ├── controller

&#x20;               │    └── PaymentController

&#x20;               │

&#x20;               ├── service

&#x20;               │    ├── PaymentService

&#x20;               │    ├── PaymentServiceImpl

&#x20;               │    └── PaymentTransactionService

&#x20;               │

&#x20;               ├── entity

&#x20;               │    └── Payment

&#x20;               │

&#x20;               ├── repositories

&#x20;               │    └── PaymentRepo

&#x20;               │

&#x20;               ├── dto

&#x20;               │    ├── PaymentRequest

&#x20;               │    └── PaymentResponse

&#x20;               │

&#x20;               └── Constants

&#x20;                    └── PaymentStatus

🚀 Running the Project

Requirements

Java 17+

Maven

MySQL



Create the database:



CREATE DATABASE payment\_db;



Configure the database connection in:



application.properties



Then run:



mvn spring-boot:run

📡 API

Create Payment

POST /payments



Headers:



Content-Type: application/json

Idempotency-Key: PAYMENT-12345



Body:



{

&#x20; "userId": 101,

&#x20; "amount": 500

}

🎓 Interview Concepts Demonstrated



This project provides hands-on experience with:



Idempotency

Duplicate payment prevention

Race conditions

Concurrent requests

Database constraints

Unique indexes

Transactions

Transaction boundaries

Spring Data JPA transactions

Hibernate

Exception propagation

Commit and rollback

Data integrity

API validation

JMeter concurrency testing

Production failure scenarios

📈 Version Summary

Version	Focus

V1	Basic payment implementation

V2	Reproduce duplicate payment

V3	Naive duplicate check

V4	Idempotency key

V5	Database UNIQUE constraint

V6	Transaction internals

V7	Production hardening

🏁 Final Outcome



The project started with:



Simple Payment API



and evolved into:



Idempotent Payment Processing

&#x20;       +

Database Protection

&#x20;       +

Transaction Boundary

&#x20;       +

Concurrency Testing

&#x20;       +

Edge Case Handling



The main engineering lesson:



Application logic can detect problems, but critical correctness guarantees should ultimately be enforced by the database.

