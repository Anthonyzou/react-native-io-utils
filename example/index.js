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

class Example extends Component {
  constructor(a,b){
    super(a,b)
    this.state = {
    }
    console.log(I)
  }
  render() {
    return (
      <ScrollView>
        <TouchableHighlight onPress={()=>{
              I.file()
          }}>
          <Text>
            file
          </Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={()=>{
            I.image()
          }}>
          <Text>
            image
          </Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={()=>{
            I.video()
          }}>
          <Text>
            video
          </Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={()=>{

          }}>
          <Text>
            hello
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
