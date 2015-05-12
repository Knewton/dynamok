### Dynamok Pages

This branch contains the source to build the Dynamok static page.

### Build Instructions

The pages are based off of the [Hyde Jekyll](https://github.com/poole/hyde) theme.  Building the web pages requires Ruby and Bundler.

To build, make sure you have Ruby and Bundler installed.  You can install Bundler by running

gem install bundler

Once Bundler has been installed, install dependencies by running

bundler install

The site can then be built and served locally by running

bundler exec jekyll serve

Navigate to http://localhost:4000/dynamok to view the site.