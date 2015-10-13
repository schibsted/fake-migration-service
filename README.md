# Fake Migration Service

Used to simulate an actual clients migration service.

## Get user

Curl: 

    curl -X GET -H "Content-Type: application/json" \
                -H "X-Auth: BEEFC4FFEE" \
                'https://fast-inlet-7431.herokuapp.com/?email=email@domain.com'
                
### Simulate errors

The behaviour of the fake migration service can be controlled using
sub-addressing. By appending `+tag` to the user part of the address,
the behaviour can be controlled in the following way:

 * `+delayN`: Wait N milliseconds before responding, e.g. `+delay2500` to wait 2.5 s.
 * `+invalidlocale`: Return an invalid locale in the user data.
 * `+invalidphone`: Return an invalid phone number format in the user data.
 * `+invalidsex`: Return invalid sex data in the user data.
 * `+invalidtimezone`: Return an invalid timezone in the user data.
 * `+modifyemail`: Modify the email address in the user data.

These can also be combined by separating multiple tags with dashes.
To delay the response and return an invalid timezone, the requester
can send a request for `jane.doe+delay2500-invalidtimezone@example.com`.

### Deploy

Create a heroku account, download the toolbelt and follow the steps below:

    $ heroku login
    $ heroku create
    
Heroku tool has now added a new git remote that we push our source to:
 
    $ git push heroku master
    
Check if the application is already running

    $ heroku ps
    $ heroku open
