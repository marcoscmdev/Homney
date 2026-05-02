<?php 
/* 
   Conceptos previos: 
   
   Parámetros a ajustar el Fichero PHP.INI. (Ubicacion del archivo: http://blog.unelink.es/wiki/php/donde-esta-mi-archivo-php-ini/),
   --------------------------------------------
    
   - file_uploads = on              .Pemite/Deniega la subida de ficheros al servidor desde el cliente.
   - post_max_size = 8M             .Ajusta el total de Megabytes que el servidor puede recibir por POST
   - upload_max_filesize = 2M 		.Ajusta el tamaño máximo permitido del fichero a subir.

   Formulario HTML o petición HTTP
   --------------------------------

   - enctype="multipart/form-data" .Necesario para subir en un mismo formulario/petición datos y archivos. 

   Script PHP
   ----------
 
   Al recibir el fichero, PHP crea una serie de celdas en el array $_FILES:

   $_FILES['nombreInputFormulario']['name'] : El nombre original del fichero 
                                              en la máquina cliente.

   $_FILES['nombreInputFormulario']['type'] : El tipo mime del fichero (si el navegador 
                                              lo proporciona. Un ejemplo podría ser "image/gif".

   $_FILES['nombreInputFormulario']['size'] : El tamaño en bytes del fichero recibido.

   $_FILES['nombreInputFormulario']['tmp_name'] : El nombre del fichero temporal que se 
                                                  utiliza para almacenar en el servidor
                                                  el archivo recibido. 
												  
   $_FILES['nombreInputFormulario']['error'] : Código del error que se ha producido al subir el fichero.


*/


require("conexion.php");

 
function nombre_directorio_actual() { // ruta para llegar a este fichero

    $dirname = dirname($_SERVER['PHP_SELF']);
    return ($dirname == '/' ||  $dirname=="\\") ? '' : substr($dirname,1);
 
 }

 /* Generamos id de la base de datos que se usará como nombre de archivo */
 function get_id_fichero($con,$usuario){
        $consulta_insert = "INSERT INTO `ficheros` (`id`, `url`, `usuario`) VALUES (NULL, '', '$usuario');";

        if (!mysqli_query($con,$consulta_insert)) {
            die_por_fallo_en_consulta($consulta_insert,$con);
        }

        $id = mysqli_insert_id($con); // Obtenemos id de columna AUTO_INCREMENT
        if ($id<0) {
             die_por_fallo_en_sintaxis_peticion("Error al obtener el valor autoincremental.");
        }
        else {
         return $id;
        }
 }

 function muestraErrorUpload($numError) {
	switch ($numError)
	{ case 1:
           	return 'El fichero supera el tama&ntilde;o m&aacute;ximo permitido de '. 
           	        ini_get('upload_max_filesize') . ".";
           	break;
	  case 2://Ej:<input type="hidden" name="MAX_FILE_SIZE" value="100000">
           	return 'El fichero supera el tama&ntilde;o m&aacute;ximo permitido por el formulario';
           	break;
	  case 3:
           	return 'El fichero no se ha recibido correctamente';
           	break;
	  case 4:
           	return 'El nombre de fichero no se ha especificado';
               break;
      default:
            return 'Error desconocido';
 	} 
 }


 if(isset($_FILES['fichero']) && $_FILES['fichero']['error']!= 0) { // !=0, Indica que se ha producido un error
    $codigo_error = isset($_FILES['fichero'])?$_FILES['fichero']['error']:'-1';
     die_por_fallo_en_sintaxis_peticion('Se ha producido un ERROR al subir el fichero, codigo: ' .   muestraErrorUpload($codigo_error)); 
 }

 if($_SERVER['REQUEST_METHOD']=='POST' && isset($_POST['usuario']) && isset($_FILES['fichero']) ){
     
    $usuario_actual=$_POST['usuario'];

    // id en la base de datos que se usa como nombre de archivo.
    $id_fichero = get_id_fichero($conexion,$usuario_actual);

    //Obtenemos la extensión del fichero
    $fileinfo = pathinfo($_FILES['fichero']['name']); 
    $extension = !isset($fileinfo['extension'])?'sinextension':$fileinfo['extension']; 
    
    // Ajustamos RUTA carpetas padre y Creamos carpetas/directorios si no existen.
    $upload_path = 'uploads/'. $usuario_actual . '/';
    if (!is_dir($upload_path))  {
        if(!@mkdir($upload_path, 0777, true)) {
             die_por_fallo_en_sintaxis_peticion("Error al crear el directorio: $upload_path " );
        }
    }
    $upload_path_para_url = 'uploads/'. urlencode($usuario_actual) . '/';
    // Ajustamos URL padre
    $nda = nombre_directorio_actual() ;
    $nombre_servidor = $_SERVER['SERVER_NAME']==='10.0.2.2'?'localhost':$_SERVER['SERVER_NAME'];
    $upload_url = 'http://'. $nombre_servidor .    '/'. $nda.  (strlen($nda)>0 ? '/' : '') .  $upload_path_para_url; 
    

    // Ajustamos RUTA y URL completas para acceder al fichero
    $file_url = $upload_url . $id_fichero . '.' . $extension; 
    $file_path = $upload_path . $id_fichero . '.'. $extension; 
    
    // Grabamos fichero y actualizamos base de datos.
    try {
        // guardamos fichero
        @move_uploaded_file($_FILES['fichero']['tmp_name'],$file_path);
        $sql = "UPDATE `ficheros` SET `url`= '$file_url', `usuario`= '$usuario_actual', `nombre_original`='". $_FILES['fichero']['name'] . "' WHERE `id`= '$id_fichero'";
 
        if(!mysqli_query($conexion,$sql)){
            die_por_fallo_en_consulta($sql,$conexion);
        }
    }
    catch(Exception $e){
         die_por_fallo_en_sintaxis_peticion($e->getMessage());
    } 
    
    // TODO HA IDO BIEN
        $respuesta[STATUS]=SUCCESS;
            $datos['url']=$file_url;
            $datos['ruta']=$file_path;
            $datos['id']= $id_fichero;
            $datos['nombre_original']=$_FILES['fichero']['name'];
        $respuesta[DATA]=$datos; 
        header("Content-type: application/json");
        echo json_encode($respuesta);
 }
 else{
    die_por_fallo_en_sintaxis_peticion(false);
}
 



?>