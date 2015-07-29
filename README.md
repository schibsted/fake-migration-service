# Fake Migration Service

Used to simulate an actual clients migration service.

## Get user

Curl: 

    curl -X GET -H "Content-Type: application/json" \
                -H "X-Auth: BEEFC4FFEE" \
                'https://fast-inlet-7431.herokuapp.com/?email=email@domain.com'

## Heroku

Currently running on `michael-stockli` account. 

[https://fast-inlet-7431.herokuapp.com](https://fast-inlet-7431.herokuapp.com/)

### Deploy

Create a heroku account, download the toolbelt and follow the steps below:

    $ heroku login
    $ heroku create
    
Heroku tool has now added a new git remote that we push our source to:
 
    $ git push heroku master
    
Check if the application is already running

    $ heroku ps
    $ heroku open
