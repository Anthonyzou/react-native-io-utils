/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';
import React, {
  AppRegistry,
  Component,
  StyleSheet,
  Text,
  ScrollView,
  Dimensions,
  TouchableHighlight,
  TextInput,
} from 'react-native';

import {Actions, Router, Route, Schema, Animations, TabBar} from 'react-native-router-flux'
import _ from 'lodash'
import I from './ioUtils.js'
import Path from 'path'

class Example extends Component {
  constructor(a,b){
    super(a,b)
    this.state = {
    }
  }
  render() {
    return (
      <ScrollView>
        <TouchableHighlight onPress={()=>{
              I.file((args)=>{
                const filePath = Path.parse(args.path)
                const params = {
                  uploadUrl: 'http://10.0.3.2:3000',
                  method: 'POST', // default 'POST',support 'POST' and 'PUT'
                  headers: {'Accept': 'application/json',},
                  fields: {
                  },
                  //for a path object with path '/a/b/cd.efg'
                  file:
                    {
                      name: 'file',
                      // this   is cd.efg
                      filename: filePath.base, // require, file name
                      // this   is /a/b/cd.efg
                      filepath: args.path, // require, file absolute path
                      uri: args.uri,
                      // filetype : filetype, // options, if none, will get mimetype from `filepath` extension
                    }
                }
                console.log(params,args);
                I.upload(params, ()=>{console.log('start')})
                .then(console.log.bind(console))
                .catch(console.log.bind(console))
                .done()
              })
          }}>
          <Text>
            file
          </Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={()=>{
            I.image((args)=>{
              console.log(args)
            })
          }}>
          <Text>
            image
          </Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={()=>{
            I.video((args)=>{
              console.log(args)
            })
          }}>
          <Text>
            video
          </Text>
        </TouchableHighlight>

      </ScrollView>
    );
  }
}

class main extends Component{
  render(){
    return (
      <Router hideNavBar={true}>
        <Route name="Main" type="reset" component={Example}/>
      </Router>
    )
  }
}

const {height, width} = Dimensions.get('window');
const styles = StyleSheet.create({
  container: {
  },
  image:{
    height: 250
  }
});

AppRegistry.registerComponent('example', () => main);
